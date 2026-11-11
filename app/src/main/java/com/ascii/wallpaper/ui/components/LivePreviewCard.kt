package com.ascii.wallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ascii.wallpaper.R
import com.ascii.wallpaper.data.AsciiSettings
import com.ascii.wallpaper.ui.theme.TerminalAccent
import com.ascii.wallpaper.ui.theme.TerminalBackground
import com.ascii.wallpaper.ui.theme.TerminalBorder
import com.ascii.wallpaper.ui.theme.TerminalCard
import com.ascii.wallpaper.ui.theme.TerminalTextDim

/**
 * Live preview at the very top of the screen. It hosts [AsciiPreviewView],
 * which drives the same [com.ascii.wallpaper.engine.AsciiRenderer] as the
 * wallpaper service.
 */
@Composable
fun LivePreviewCard(
    settings: AsciiSettings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalCard)
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TerminalAccent),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.preview_label),
                color = TerminalTextDim,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${settings.fps} FPS",
                color = TerminalAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalBackground),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> AsciiPreviewView(context) },
                update = { view -> view.setSettings(settings) },
            )
            // terminal frame corners
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(10.dp)
                    .border(1.dp, TerminalAccent.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(10.dp)
                    .border(1.dp, TerminalAccent.copy(alpha = 0.35f)),
            )
        }
    }
}
