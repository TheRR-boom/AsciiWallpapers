package com.ascii.wallpaper.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.data.SettingsRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live wallpaper that draws 3D figures out of ASCII glyphs.
 *
 * The frame loop runs on a dedicated [HandlerThread] rather than on
 * Choreographer: a wallpaper engine has no window, and on many devices the
 * app frame clock stops delivering vsync after the first frame, which freezes
 * the wallpaper image. A timed loop is independent of that and lets us pace
 * ourselves exactly at the configured FPS.
 *
 * Contract:
 *  - settings are re-read live from SharedPreferences, no restart needed;
 *  - the loop is torn down on `onVisibilityChanged(false)`, so the wallpaper
 *    costs no CPU while it is off-screen;
 *  - the unlock animation is a radial burst triggered by USER_PRESENT.
 */
class AsciiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AsciiEngine()

    private inner class AsciiEngine : Engine() {

        /** Touched only by the render thread. */
        private val renderer = AsciiRenderer()

        private val renderThread = HandlerThread("ascii-wallpaper-render").apply { start() }
        private val renderHandler = Handler(renderThread.looper)

        @Volatile
        private var settings: AsciiSettings = AsciiSettings.DEFAULT

        @Volatile
        private var burstStartNanos: Long = -1L

        private val running = AtomicBoolean(false)
        private var visible = false
        private var appliedSettings: AsciiSettings? = null
        private var lastFrameNanos = 0L

        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                settings = SettingsRepository.read(this@AsciiWallpaperService)
            }

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT && settings.unlockAnimation) {
                    burstStartNanos = System.nanoTime()
                }
            }
        }

        /** Self-rescheduling frame pump, pinned to the configured FPS. */
        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!running.get()) return
                val startedAt = System.nanoTime()
                val current = settings
                if (appliedSettings != current) {
                    renderer.update(current)
                    appliedSettings = current
                }
                drawFrame(startedAt, current)

                val budget = 1_000_000_000L / current.fps.coerceIn(1, 240)
                val spent = System.nanoTime() - startedAt
                renderHandler.postDelayed(this, (budget - spent).coerceAtLeast(0L))
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(false)
            settings = SettingsRepository.read(this@AsciiWallpaperService)
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
            // Surface may be resized while we are running; the loop picks up the
            // new canvas size on its next frame.
            if (visible) startRendering()
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
            renderThread.quitSafely()
            super.onDestroy()
        }

        private fun startRendering() {
            if (!running.compareAndSet(false, true)) return
            renderHandler.post {
                lastFrameNanos = System.nanoTime()
                if (settings.unlockAnimation) burstStartNanos = lastFrameNanos
                renderHandler.removeCallbacks(frameRunnable)
                renderHandler.post(frameRunnable)
            }
        }

        private fun stopRendering() {
            running.set(false)
            renderHandler.removeCallbacks(frameRunnable)
        }

        private fun drawFrame(nowNanos: Long, current: AsciiSettings) {
            if (!running.get()) return
            val holder = surfaceHolder
            if (!holder.surface.isValid) return

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val width = canvas.width
                val height = canvas.height
                if (width <= 0 || height <= 0) return

                val dt = if (lastFrameNanos == 0L) {
                    0f
                } else {
                    (nowNanos - lastFrameNanos) / 1_000_000_000f
                }
                lastFrameNanos = nowNanos

                renderer.draw(canvas, width, height, dt, burstProgress(nowNanos, current))
            } catch (_: IllegalArgumentException) {
                // Surface vanished between the validity check and lockCanvas().
            } catch (_: IllegalStateException) {
                // Same, from a stale surface holder.
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
        }

        private fun burstProgress(nowNanos: Long, current: AsciiSettings): Float {
            if (!current.unlockAnimation) return 0f
            val start = burstStartNanos
            if (start <= 0L) return 0f
            val progress = (nowNanos - start).toFloat() / BURST_DURATION_NANOS
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
