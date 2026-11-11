package com.ascii.wallpaper.ui.components

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.engine.AsciiRenderer

/**
 * A small View that runs the real wallpaper renderer at up to 60 fps, so the
 * settings screen shows exactly the animation the wallpaper will show.
 */
class AsciiPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val renderer = AsciiRenderer()
    private val choreographer = Choreographer.getInstance()

    private var settings: AsciiSettings = AsciiSettings.DEFAULT
    private var startNanos = 0L
    private var lastDrawNanos = 0L
    private var running = false
    private var scheduled = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            scheduled = false
            if (!running) return
            if (frameTimeNanos - lastDrawNanos >= FRAME_BUDGET_NANOS) {
                lastDrawNanos = frameTimeNanos
                invalidate()
            }
            schedule()
        }
    }

    init {
        setBackgroundColor(AsciiRenderer.BACKGROUND_COLOR)
        renderer.update(settings)
    }

    fun setSettings(newSettings: AsciiSettings) {
        if (newSettings == settings) return
        settings = newSettings
        renderer.update(newSettings)
        invalidate()
    }

    private fun schedule() {
        if (!scheduled && running && isAttachedToWindow) {
            scheduled = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    private fun startLoop() {
        if (running) return
        running = true
        startNanos = System.nanoTime()
        lastDrawNanos = startNanos
        schedule()
    }

    private fun stopLoop() {
        running = false
        if (scheduled) {
            choreographer.removeFrameCallback(frameCallback)
            scheduled = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startLoop() else stopLoop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val elapsed = (System.nanoTime() - startNanos) / 1_000_000_000f
        renderer.draw(canvas, width, height, elapsed, 0f)
    }

    private companion object {
        const val FRAME_BUDGET_NANOS = 16_666_667L
    }
}
