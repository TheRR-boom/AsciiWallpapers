package com.ascii.wallpaper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.ascii.wallpaper.data.ShapeType
import com.ascii.wallpaper.ui.theme.TerminalAccent
import com.ascii.wallpaper.ui.theme.TerminalText
import com.ascii.wallpaper.ui.theme.TerminalTextDim

/** Radio list of the five figures; the selected row is highlighted in green. */
@Composable
fun ShapeSelector(
    selected: ShapeType,
    modifier: Modifier = Modifier,
    onSelect: (ShapeType) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ShapeType.entries.forEach { shape ->
            val active = shape == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(shape) }
                    .padding(vertical = 2.dp),
            ) {
                RadioButton(
                    selected = active,
                    onClick = { onSelect(shape) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = TerminalAccent,
                        unselectedColor = TerminalTextDim,
                    ),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = stringResource(shape.labelRes),
                    color = if (active) TerminalAccent else TerminalText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
