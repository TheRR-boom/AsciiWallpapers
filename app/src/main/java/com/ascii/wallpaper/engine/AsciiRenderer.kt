package com.ascii.wallpaper.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.ascii.wallpaper.data.AsciiSettings
import java.util.Arrays
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure rendering core: 3D rotation -> perspective projection -> glyph picking.
 *
 * The same instance logic powers both the wallpaper surface and the in-app live
 * preview, so what the user sees in the settings screen is exactly what the
 * wallpaper draws.
 *
 * Not thread safe on purpose: the wallpaper engine and the preview view both
 * call it from the main thread. All buffers are pre-allocated and reused, so a
 * frame does not allocate except for the transient sort keys (also reused).
 */
class AsciiRenderer {

    /** Brightness ramp: dim characters first, dense ones last. */
    private val ramp = " .,:;!*+=xX#%@" // 15 glyphs

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private var params: AsciiSettings = AsciiSettings.DEFAULT
    private var cloud: PointCloud = PointCloud(0)
    private var signature: String = ""

    private var screenX = FloatArray(0)
    private var screenY = FloatArray(0)
    private var depth = FloatArray(0)
    private var brightness = FloatArray(0)
    private var order = LongArray(0)

    /**
     * Applies a new configuration snapshot. Geometry is only regenerated when a
     * knob that actually changes the point cloud moved.
     */
    fun update(settings: AsciiSettings) {
        params = settings
        val glyphCount = glyphCountFor(settings.density)
        val newSignature = buildString {
            append(settings.shape.storageKey)
            append('|').append(glyphCount)
            append('|').append(settings.thickness)
        }
        if (newSignature == signature) return

        signature = newSignature
        cloud = ShapeFactory.build(
            shape = settings.shape,
            glyphCount = glyphCount,
            thickness = settings.thickness,
            seed = settings.shape.ordinal * 7919L + 17L,
        )
        ensureBuffers(cloud.size)
    }

    /**
     * Draws one frame.
     *
     * @param timeSeconds monotonic time, drives rotation.
     * @param burst 0..1 unlock animation progress; 0 means idle.
     */
    fun draw(canvas: Canvas, width: Int, height: Int, timeSeconds: Float, burst: Float) {
        canvas.drawColor(BACKGROUND_COLOR)

        val count = cloud.size
        if (count == 0 || width <= 0 || height <= 0) return
        if (screenX.size < count) ensureBuffers(count)

        val speed = params.speed.toFloat()
        // 13 (default) -> ~0.29 rad/s, 30 -> ~0.66 rad/s
        val yaw = timeSeconds * speed * 0.022f
        val pitch = 0.30f + sin(timeSeconds * 0.21f) * 0.22f
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)

        val minDim = min(width, height).toFloat()
        val focal = minDim * 0.92f
        val camZ = 3.4f
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val zoom = 1f + burst * 0.07f
        val invHalfDiag = 1f / (minDim * 0.5f)

        val srcX = cloud.x
        val srcY = cloud.y
        val srcZ = cloud.z
        val shade = cloud.shade

        for (i in 0 until count) {
            val x = srcX[i]
            val y = srcY[i]
            val z = srcZ[i]

            val rx = x * cosYaw + z * sinYaw
            val rz = -x * sinYaw + z * cosYaw
            val ry = y * cosPitch - rz * sinPitch
            val rz2 = y * sinPitch + rz * cosPitch

            val inv = 1f / (rz2 + camZ)
            val sx = centerX + rx * focal * inv * zoom
            val sy = centerY + ry * focal * inv * zoom
            screenX[i] = sx
            screenY[i] = sy
            depth[i] = rz2

            var b = 0.35f + 0.65f * ((1.8f - rz2) / 3.6f).coerceIn(0f, 1f)
            b *= 0.55f + 0.45f * shade[i]

            if (burst > 0f) {
                val dx = sx - centerX
                val dy = sy - centerY
                val dist = sqrt(dx * dx + dy * dy) * invHalfDiag
                b += (1f - burst) * 0.6f * sin((1f - dist) * 9f - burst * 15f)
            }
            brightness[i] = b.coerceIn(0f, 1f)
        }

        // Painter's algorithm, packed into a primitive LongArray so the sort
        // never boxes and never allocates per frame.
        for (i in 0 until count) {
            val quantized = (((depth[i] + 2.2f) * 800f).roundToInt()).coerceIn(0, 4094)
            order[i] = ((4094 - quantized).toLong() shl 22) or i.toLong()
        }
        Arrays.sort(order, 0, count)

        val cell = width / params.density.toFloat()
        paint.textSize = (cell * 1.35f * (0.78f + 0.22f * params.thickness)).coerceIn(5f, 80f)
        paint.color = params.charColor.argb

        val rampLength = ramp.length
        for (k in 0 until count) {
            val index = (order[k] and 0x3FFFFF).toInt()
            val b = brightness[index]
            val glyph = (b * (rampLength - 1)).roundToInt().coerceIn(0, rampLength - 1)
            if (glyph == 0) continue // space: nothing to draw
            paint.alpha = (40f + 215f * b).toInt().coerceIn(0, 255)
            canvas.drawText(ramp, glyph, glyph + 1, screenX[index], screenY[index], paint)
        }
    }

    private fun ensureBuffers(size: Int) {
        if (screenX.size < size) {
            screenX = FloatArray(size)
            screenY = FloatArray(size)
            depth = FloatArray(size)
            brightness = FloatArray(size)
            order = LongArray(size)
        }
    }

    private fun glyphCountFor(density: Int): Int =
        (density * density / 12).coerceIn(300, 2200)

    companion object {
        /** Deep black with a green undertone, matches the UI background. */
        const val BACKGROUND_COLOR: Int = 0xFF0A0F0A.toInt()
    }
}
