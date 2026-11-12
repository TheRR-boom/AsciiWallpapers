package com.ascii.wallpaper.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.data.ShapeType
import java.util.Arrays
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * ASCII 3D renderer.
 *
 * Pipeline per frame:
 *   1. advance the camera angles (angleX/angleY) by dt — independent of frame rate;
 *   2. per point: optional differential spin around the shape's own axis
 *      (accretion disk: inner rings rotate faster than outer ones);
 *   3. rotate: R = Rx * Ry applied as explicit matrices;
 *   4. perspective projection: divide by (z + cameraDistance), so far glyphs
 *      converge to the centre and near glyphs spread out;
 *   5. depth shading: near glyphs are brighter, bigger and drawn with dense
 *      characters (`@`, `#`), far glyphs are dim, small and drawn with `.`;
 *   6. painter's sort far -> near so near glyphs overdraw far ones.
 *
 * The renderer owns its animation clock: [draw] takes the frame delta, not an
 * absolute timestamp, so it keeps spinning even if the host restarts its clock.
 */
class AsciiRenderer {

    /** Far -> near. The last characters are the "bright/dense" end. */
    private val ramp = ".,:;+=*xX#%@"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private var params: AsciiSettings = AsciiSettings.DEFAULT
    private var cloud: PointCloud = PointCloud(0)
    private var signature: String = ""

    // --- animation state (owned by the render thread) -----------------------
    private var angleX = 0.40f
    private var angleY = 0.0f
    private var clock = 0f
    private var spinPhase = FloatArray(0)

    // --- frame scratch buffers ---------------------------------------------
    private var screenX = FloatArray(0)
    private var screenY = FloatArray(0)
    private var depth = FloatArray(0)
    private var brightness = FloatArray(0)
    private var glyphSize = FloatArray(0)
    private var order = LongArray(0)

    /**
     * Applies a new configuration snapshot. Geometry is only rebuilt when a
     * knob that changes the point cloud actually moved.
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
        spinPhase = FloatArray(cloud.size)
    }

    /**
     * Draws one frame.
     *
     * @param deltaSeconds time since the previous frame (clamped internally).
     * @param burst 0..1 unlock animation progress; 0 means idle.
     */
    fun draw(canvas: Canvas, width: Int, height: Int, deltaSeconds: Float, burst: Float) {
        canvas.drawColor(BACKGROUND_COLOR)

        val count = cloud.size
        if (count == 0 || width <= 0 || height <= 0) return
        ensureBuffers(count)

        val dt = deltaSeconds.coerceIn(0f, MAX_DELTA_SECONDS)
        val speed = params.speed.toFloat()

        // 2) advance the camera: yaw keeps spinning, pitch tumbles (or stays a
        //    gentle tilt for the disk, which should not roll over).
        clock += dt
        angleY = wrapAngle(angleY + dt * speed * YAW_RATE)
        angleX = if (params.shape == ShapeType.BLACK_HOLE) {
            TILT_BASE + sin(clock * 0.25f) * 0.22f
        } else {
            wrapAngle(angleX + dt * speed * TUMBLE_RATE)
        }

        val cosX = cos(angleX)
        val sinX = sin(angleX)
        val cosY = cos(angleY)
        val sinY = sin(angleY)

        val minDim = min(width, height).toFloat()
        val focal = minDim * 0.78f
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val zoom = 1f + burst * 0.07f
        val invHalfDiag = 1f / (minDim * 0.5f)

        val srcX = cloud.x
        val srcY = cloud.y
        val srcZ = cloud.z
        val shade = cloud.shade
        val spinRatio = cloud.spin

        for (i in 0 until count) {
            var x = srcX[i]
            var z = srcZ[i]
            val y = srcY[i]

            // 3a) differential spin around the shape's Y axis (black hole disk).
            val ratio = spinRatio[i]
            if (ratio != 0f) {
                spinPhase[i] = wrapAngle(spinPhase[i] + dt * speed * SPIN_RATE * ratio)
                val a = spinPhase[i]
                val ca = cos(a)
                val sa = sin(a)
                val rotatedX = x * ca + z * sa
                z = -x * sa + z * ca
                x = rotatedX
            }

            // 3b) Ry then Rx, written as explicit rotation matrices.
            val x1 = x * cosY + z * sinY
            val z1 = -x * sinY + z * cosY
            val y2 = y * cosX - z1 * sinX
            val z2 = y * sinX + z1 * cosX

            // 4) perspective: divide by depth.
            val invZ = 1f / (z2 + CAMERA_DISTANCE)
            val sx = centerX + x1 * focal * invZ * zoom
            val sy = centerY + y2 * focal * invZ * zoom
            screenX[i] = sx
            screenY[i] = sy
            depth[i] = z2

            // 5) depth cues: near = bright / big / dense glyph.
            val near = ((DEPTH_HALF - z2) / (2f * DEPTH_HALF)).coerceIn(0f, 1f)
            var b = (0.12f + 0.88f * near) * (0.55f + 0.45f * shade[i])
            if (burst > 0f) {
                val dx = sx - centerX
                val dy = sy - centerY
                val dist = kotlin.math.sqrt(dx * dx + dy * dy) * invHalfDiag
                b += (1f - burst) * 0.55f * sin((1f - dist) * 9f - burst * 15f)
            }
            brightness[i] = b.coerceIn(0f, 1f)
            glyphSize[i] = 0.6f + 0.7f * near
        }

        // 6) painter's algorithm, far -> near, packed into a LongArray.
        for (i in 0 until count) {
            val quantized = (((depth[i] + DEPTH_HALF) / (2f * DEPTH_HALF)) * 4094f)
                .roundToInt().coerceIn(0, 4094)
            order[i] = ((4094 - quantized).toLong() shl 22) or i.toLong()
        }
        Arrays.sort(order, 0, count)

        val cell = width / params.density.toFloat()
        val baseSize = (cell * 1.35f * (0.78f + 0.22f * params.thickness)).coerceIn(5f, 80f)
        paint.color = params.charColor.argb

        var currentSize = -1f
        val rampLength = ramp.length
        for (k in 0 until count) {
            val index = (order[k] and 0x3FFFFF).toInt()
            val b = brightness[index]
            val glyph = (b * (rampLength - 1)).roundToInt().coerceIn(0, rampLength - 1)

            // Sorted far -> near, so the size only grows: cheap to batch.
            val size = (baseSize * glyphSize[index] * 2f).roundToInt() / 2f
            if (size != currentSize) {
                paint.textSize = size
                currentSize = size
            }
            paint.alpha = (65f + 190f * b).toInt().coerceIn(0, 255)
            canvas.drawText(ramp, glyph, glyph + 1, screenX[index], screenY[index], paint)
        }
    }

    private fun ensureBuffers(size: Int) {
        if (screenX.size >= size) return
        screenX = FloatArray(size)
        screenY = FloatArray(size)
        depth = FloatArray(size)
        brightness = FloatArray(size)
        glyphSize = FloatArray(size)
        order = LongArray(size)
    }

    private fun glyphCountFor(density: Int): Int = (density * density / 12).coerceIn(300, 2200)

    private fun wrapAngle(value: Float): Float {
        val twoPi = TWO_PI
        var v = value % twoPi
        if (v < 0f) v += twoPi
        return v
    }

    companion object {
        /** Deep black with a green undertone, matches the UI background. */
        const val BACKGROUND_COLOR: Int = 0xFF0A0F0A.toInt()

        private const val CAMERA_DISTANCE = 3.2f
        private const val DEPTH_HALF = 2.0f
        private const val MAX_DELTA_SECONDS = 0.1f
        private const val TWO_PI = 6.2831855f

        /** radians/second per speed unit (speed 13 -> ~0.72 rad/s). */
        private const val YAW_RATE = 0.055f
        private const val TUMBLE_RATE = 0.020f
        private const val SPIN_RATE = 0.25f
        private const val TILT_BASE = 0.40f
    }
}
