package com.ascii.wallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascii.wallpaper.data.CharColor
import com.ascii.wallpaper.ui.theme.TerminalAccent
import com.ascii.wallpaper.ui.theme.TerminalTextDim
import com.ascii.wallpaper.ui.theme.TerminalBorder

/** Glyph colour picker: three circles, selected one gets a green ring. */
@Composable
fun ColorSelector(
    selected: CharColor,
    modifier: Modifier = Modifier,
    onSelect: (CharColor) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        CharColor.entries.forEach { option ->
            val active = option == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(option) },
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (active) TerminalAccent.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) TerminalAccent else TerminalBorder,
                            shape = CircleShape,
                        )
                        .padding(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(option.argb))
                            .align(Alignment.Center),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(option.labelRes),
                    color = if (active) TerminalAccent else TerminalTextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
