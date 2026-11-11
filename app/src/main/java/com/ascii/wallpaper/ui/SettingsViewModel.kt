package com.ascii.wallpaper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.data.CharColor
import com.ascii.wallpaper.data.SettingsRepository
import com.ascii.wallpaper.data.ShapeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the settings state. Every edit is written to SharedPreferences right
 * away, which is what pushes it into the running wallpaper engine.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow(SettingsRepository.read(application))
    val settings: StateFlow<AsciiSettings> = _settings.asStateFlow()

    fun update(transform: (AsciiSettings) -> AsciiSettings) {
        val current = _settings.value
        val next = transform(current)
        if (next == current) return
        _settings.value = next
        SettingsRepository.write(getApplication(), next)
    }

    fun setShape(shape: ShapeType) = update { it.copy(shape = shape) }

    fun setDensity(density: Int) = update {
        it.copy(density = density.coerceIn(AsciiSettings.DENSITY_RANGE))
    }

    fun setSpeed(speed: Int) = update {
        it.copy(speed = speed.coerceIn(AsciiSettings.SPEED_RANGE))
    }

    fun setFps(fps: Int) = update { it.copy(fps = fps) }

    fun setThickness(thickness: Float) = update {
        it.copy(thickness = thickness.coerceIn(AsciiSettings.THICKNESS_RANGE))
    }

    fun setCharColor(color: CharColor) = update { it.copy(charColor = color) }

    fun setUnlockAnimation(enabled: Boolean) = update { it.copy(unlockAnimation = enabled) }
}
