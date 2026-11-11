package com.ascii.wallpaper.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Single source of truth for the wallpaper configuration.
 *
 * Everything lives in one SharedPreferences file so that the settings screen and
 * [com.ascii.wallpaper.engine.AsciiWallpaperService] (same process) observe each
 * other's writes immediately. The engine registers [observe] and re-reads the
 * snapshot on every change, which is what makes tuning "live" without restarting.
 */
object SettingsRepository {

    const val PREFS_NAME = "ascii_wallpaper_settings"

    private const val KEY_SHAPE = "shape"
    private const val KEY_DENSITY = "density"
    private const val KEY_SPEED = "speed"
    private const val KEY_FPS = "fps"
    private const val KEY_THICKNESS = "thickness"
    private const val KEY_COLOR = "char_color"
    private const val KEY_UNLOCK = "unlock_animation"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(context: Context): AsciiSettings {
        val p = prefs(context)
        val fallback = AsciiSettings.DEFAULT
        return AsciiSettings(
            shape = ShapeType.fromKey(p.getString(KEY_SHAPE, null)),
            density = p.getInt(KEY_DENSITY, fallback.density).coerceIn(AsciiSettings.DENSITY_RANGE),
            speed = p.getInt(KEY_SPEED, fallback.speed).coerceIn(AsciiSettings.SPEED_RANGE),
            fps = p.getInt(KEY_FPS, fallback.fps).let { fps ->
                if (fps in AsciiSettings.FPS_OPTIONS) fps else fallback.fps
            },
            thickness = p.getFloat(KEY_THICKNESS, fallback.thickness)
                .coerceIn(AsciiSettings.THICKNESS_RANGE),
            charColor = CharColor.fromKey(p.getString(KEY_COLOR, null)),
            unlockAnimation = p.getBoolean(KEY_UNLOCK, fallback.unlockAnimation),
        )
    }

    fun write(context: Context, settings: AsciiSettings) {
        prefs(context).edit()
            .putString(KEY_SHAPE, settings.shape.storageKey)
            .putInt(KEY_DENSITY, settings.density)
            .putInt(KEY_SPEED, settings.speed)
            .putInt(KEY_FPS, settings.fps)
            .putFloat(KEY_THICKNESS, settings.thickness)
            .putString(KEY_COLOR, settings.charColor.storageKey)
            .putBoolean(KEY_UNLOCK, settings.unlockAnimation)
            .apply()
    }

    fun observe(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun stopObserving(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}
