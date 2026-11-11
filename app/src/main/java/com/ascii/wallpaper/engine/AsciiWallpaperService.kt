package com.ascii.wallpaper.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.data.SettingsRepository

/**
 * Live wallpaper that draws 3D figures out of ASCII glyphs.
 *
 * Behaviour contract:
 *  - rendering is frame-paced by [Choreographer] and honours the FPS setting;
 *  - settings are re-read live from [SettingsRepository] (no restart needed);
 *  - the frame loop is fully torn down on `onVisibilityChanged(false)`, so the
 *    wallpaper costs no CPU while it is off-screen;
 *  - the unlock animation is an extra radial burst triggered by USER_PRESENT.
 */
class AsciiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AsciiEngine()

    private inner class AsciiEngine : Engine() {

        private val renderer = AsciiRenderer()
        private val choreographer = Choreographer.getInstance()
        private val mainHandler = Handler(Looper.getMainLooper())

        private var settings: AsciiSettings = AsciiSettings.DEFAULT
        private var visible = false
        private var drawing = false
        private var startNanos = 0L
        private var lastFrameNanos = 0L
        private var burstStartNanos = -1L

        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                settings = SettingsRepository.read(this@AsciiWallpaperService)
                renderer.update(settings)
                if (visible && !drawing) renderOnce(System.nanoTime())
            }

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT && settings.unlockAnimation) {
                    burstStartNanos = System.nanoTime()
                }
            }
        }

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!drawing) return
                val fps = settings.fps.coerceIn(1, 240)
                val budget = 1_000_000_000L / fps
                val sinceLast = frameTimeNanos - lastFrameNanos
                if (sinceLast >= budget) {
                    lastFrameNanos = frameTimeNanos - (sinceLast % budget)
                    renderOnce(frameTimeNanos)
                }
                if (drawing) choreographer.postFrameCallback(this)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(false)
            settings = SettingsRepository.read(this@AsciiWallpaperService)
            renderer.update(settings)
            SettingsRepository.observe(this@AsciiWallpaperService, prefsListener)
            ContextCompat.registerReceiver(
                this@AsciiWallpaperService,
                unlockReceiver,
                IntentFilter(Intent.ACTION_USER_PRESENT),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            if (visible) startRendering() else stopRendering()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visible && !drawing) renderOnce(System.nanoTime())
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            // Must be synchronous: after this returns the surface is gone.
            stopRendering()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopRendering()
            SettingsRepository.stopObserving(this@AsciiWallpaperService, prefsListener)
            runCatching { unregisterReceiver(unlockReceiver) }
            mainHandler.removeCallbacksAndMessages(null)
            super.onDestroy()
        }

        private fun startRendering() {
            if (drawing) return
            drawing = true
            startNanos = System.nanoTime()
            lastFrameNanos = startNanos
            if (settings.unlockAnimation) burstStartNanos = startNanos
            choreographer.postFrameCallback(frameCallback)
        }

        private fun stopRendering() {
            if (!drawing) return
            drawing = false
            choreographer.removeFrameCallback(frameCallback)
        }

        private fun renderOnce(frameTimeNanos: Long) {
            if (!visible || startNanos == 0L) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val width = canvas.width
                val height = canvas.height
                if (width <= 0 || height <= 0) return

                val elapsed = (frameTimeNanos - startNanos) / 1_000_000_000f
                val burst = burstProgress(frameTimeNanos)
                renderer.draw(canvas, width, height, elapsed, burst)
            } catch (ignored: IllegalArgumentException) {
                // Surface was destroyed between the visibility callback and now.
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
        }

        private fun burstProgress(frameTimeNanos: Long): Float {
            if (!settings.unlockAnimation) return 0f
            val start = burstStartNanos
            if (start <= 0L) return 0f
            val progress = (frameTimeNanos - start).toFloat() / BURST_DURATION_NANOS
            if (progress >= 1f) {
                burstStartNanos = -1L
                return 0f
            }
            return progress.coerceIn(0f, 1f)
        }
    }

    companion object {
        private const val BURST_DURATION_NANOS = 1_400_000_000f
    }
}
