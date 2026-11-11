package com.ascii.wallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascii.wallpaper.ui.theme.TerminalAccent
import com.ascii.wallpaper.ui.theme.TerminalBlack
import com.ascii.wallpaper.ui.theme.TerminalBorder
import com.ascii.wallpaper.ui.theme.TerminalTextDim

/** Segmented FPS selector: the active cell is filled with neon green. */
@Composable
fun FpsSelector(
    selected: Int,
    options: List<Int>,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { fps ->
            val active = fps == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) TerminalAccent else TerminalBlack)
                    .border(
                        width = 1.dp,
                        color = if (active) TerminalAccent else TerminalBorder,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(fps) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = fps.toString(),
                    color = if (active) TerminalBlack else TerminalTextDim,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
