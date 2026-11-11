package com.ascii.wallpaper.data

import androidx.annotation.StringRes
import com.ascii.wallpaper.R

/** The five 3D figures rendered by the engine. */
enum class ShapeType(
    val storageKey: String,
    @StringRes val labelRes: Int,
) {
    PASTA("pasta", R.string.shape_pasta),
    CUBES("cubes", R.string.shape_cubes),
    BIG_CUBE("big_cube", R.string.shape_big_cube),
    DNA("dna", R.string.shape_dna),
    BLACK_HOLE("black_hole", R.string.shape_black_hole);

    companion object {
        fun fromKey(key: String?): ShapeType =
            entries.firstOrNull { it.storageKey == key } ?: PASTA
    }
}

/** Glyph tint presets. */
enum class CharColor(
    val storageKey: String,
    @StringRes val labelRes: Int,
    val argb: Int,
) {
    WHITE("white", R.string.color_white, 0xFFFFFFFF.toInt()),
    GREEN("green", R.string.color_green, 0xFF00FF41.toInt()),
    AMBER("amber", R.string.color_amber, 0xFFFFB000.toInt());

    companion object {
        fun fromKey(key: String?): CharColor =
            entries.firstOrNull { it.storageKey == key } ?: GREEN
    }
}

/**
 * Immutable snapshot of every user-tunable knob.
 *
 * The same object is produced by [SettingsRepository] for the settings screen
 * and for the wallpaper engine, so the live preview and the wallpaper can never
 * drift apart.
 */
data class AsciiSettings(
    val shape: ShapeType = ShapeType.PASTA,
    val density: Int = 100,
    val speed: Int = 13,
    val fps: Int = 60,
    val thickness: Float = 1.0f,
    val charColor: CharColor = CharColor.GREEN,
    val unlockAnimation: Boolean = true,
) {
    companion object {
        val DENSITY_RANGE = 40..200
        val SPEED_RANGE = 0..30
        val THICKNESS_RANGE = 0.5f..2.0f
        val FPS_OPTIONS = listOf(30, 45, 60, 90, 120)

        val DEFAULT = AsciiSettings()
    }
}
