package com.ascii.wallpaper.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascii.wallpaper.R
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.ui.components.ColorSelector
import com.ascii.wallpaper.ui.components.FpsSelector
import com.ascii.wallpaper.ui.components.GreenSlider
import com.ascii.wallpaper.ui.components.LivePreviewCard
import com.ascii.wallpaper.ui.components.ShapeSelector
import com.ascii.wallpaper.ui.components.TerminalCard
import com.ascii.wallpaper.ui.theme.TerminalAccent
import com.ascii.wallpaper.ui.theme.TerminalBackground
import com.ascii.wallpaper.ui.theme.TerminalBlack
import com.ascii.wallpaper.ui.theme.TerminalBorder
import com.ascii.wallpaper.ui.theme.TerminalTextDim
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onInstallWallpaper: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val densityRange = AsciiSettings.DENSITY_RANGE.first.toFloat()..AsciiSettings.DENSITY_RANGE.last.toFloat()
    val speedRange = AsciiSettings.SPEED_RANGE.first.toFloat()..AsciiSettings.SPEED_RANGE.last.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        TerminalHeader()
        Spacer(Modifier.height(14.dp))

        LivePreviewCard(settings = settings)

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_shape)) {
            ShapeSelector(
                selected = settings.shape,
                onSelect = viewModel::setShape,
            )
        }

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_density)) {
            GreenSlider(
                label = stringResource(R.string.section_density),
                valueText = stringResource(R.string.value_density, settings.density),
                value = settings.density.toFloat(),
                valueRange = densityRange,
                steps = AsciiSettings.DENSITY_RANGE.last - AsciiSettings.DENSITY_RANGE.first - 1,
                onValueChange = { viewModel.setDensity(it.roundToInt()) },
            )
        }

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_speed)) {
            GreenSlider(
                label = stringResource(R.string.section_speed),
                valueText = stringResource(R.string.value_speed, settings.speed),
                value = settings.speed.toFloat(),
                valueRange = speedRange,
                steps = AsciiSettings.SPEED_RANGE.last - AsciiSettings.SPEED_RANGE.first - 1,
                onValueChange = { viewModel.setSpeed(it.roundToInt()) },
            )
        }

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_fps)) {
            FpsSelector(
                selected = settings.fps,
                options = AsciiSettings.FPS_OPTIONS,
                onSelect = viewModel::setFps,
            )
        }

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_thickness)) {
            GreenSlider(
                label = stringResource(R.string.section_thickness),
                valueText = stringResource(
                    R.string.value_thickness,
                    String.format(Locale.US, "%.1f", settings.thickness),
                ),
                value = settings.thickness,
                valueRange = AsciiSettings.THICKNESS_RANGE,
                steps = 14,
                onValueChange = { raw ->
                    val snapped = (raw * 10f).roundToInt() / 10f
                    viewModel.setThickness(snapped)
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_color)) {
            ColorSelector(
                selected = settings.charColor,
                onSelect = viewModel::setCharColor,
            )
        }

        Spacer(Modifier.height(12.dp))
        TerminalCard(title = stringResource(R.string.section_unlock)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.section_unlock_hint),
                        color = TerminalTextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = settings.unlockAnimation,
                    onCheckedChange = viewModel::setUnlockAnimation,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TerminalBlack,
                        checkedTrackColor = TerminalAccent,
                        checkedBorderColor = TerminalAccent,
                        uncheckedThumbColor = TerminalTextDim,
                        uncheckedTrackColor = TerminalBackground,
                        uncheckedBorderColor = TerminalBorder,
                    ),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onInstallWallpaper,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TerminalAccent,
                contentColor = TerminalBlack,
            ),
        ) {
            Text(
                text = stringResource(R.string.install_wallpaper),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.install_hint),
            color = TerminalTextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        StatusFooter(settings)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun TerminalHeader() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.header_title),
                color = TerminalAccent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "█",
                color = TerminalAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                modifier = Modifier.alpha(cursorAlpha),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.header_subtitle),
            color = TerminalTextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun StatusFooter(settings: AsciiSettings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "shape=${settings.shape.storageKey} fps=${settings.fps}",
            color = TerminalTextDim.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(5.dp)
                    .background(TerminalAccent, RoundedCornerShape(50)),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "ENGINE READY",
                color = TerminalAccent.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}
