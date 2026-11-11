package com.ascii.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ascii.wallpaper.engine.AsciiWallpaperService
import com.ascii.wallpaper.ui.SettingsScreen
import com.ascii.wallpaper.ui.SettingsViewModel
import com.ascii.wallpaper.ui.theme.AsciiWallpapersTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AsciiWallpapersTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onInstallWallpaper = ::openWallpaperPicker,
                )
            }
        }
    }

    /**
     * Opens the system live wallpaper picker pre-selected on our engine, with a
     * graceful fallback for OEMs (Samsung/One UI in particular) that do not
     * expose ACTION_CHANGE_LIVE_WALLPAPER.
     */
    private fun openWallpaperPicker() {
        val component = ComponentName(this, AsciiWallpaperService::class.java)

        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        try {
            startActivity(direct)
            return
        } catch (_: ActivityNotFoundException) {
            // fall through
        } catch (_: SecurityException) {
            // fall through
        }

        val chooser = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
        try {
            startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.wallpaper_picker_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}
