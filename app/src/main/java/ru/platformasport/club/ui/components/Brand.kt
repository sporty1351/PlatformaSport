package ru.platformasport.club.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.TileSilver
import ru.platformasport.club.ui.theme.TileWhite

/**
 * Знак клуба: сетка 3×3 из скруглённых плиток, повёрнутая на 45°,
 * с красной плиткой в центре. Рисуется, а не растрируется, поэтому
 * одинаково хорошо смотрится и в 24 dp, и на всю ширину карточки.
 */
@Composable
fun RhombusMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    accent: Color = PlatformaRed,
    light: Color = TileWhite,
    dim: Color = TileSilver,
    alpha: Float = 1f
) {
    Canvas(modifier.size(size)) { drawRhombusMark(accent, light, dim, alpha) }
}

fun DrawScope.drawRhombusMark(
    accent: Color = PlatformaRed,
    light: Color = TileWhite,
    dim: Color = TileSilver,
    alpha: Float = 1f,
    centerOffset: Offset = Offset.Zero
) {
    val d = size.minDimension
    val tile = d * 0.19f
    val gap = tile * 0.16f
    val step = tile + gap
    val cx = center.x + centerOffset.x
    val cy = center.y + centerOffset.y

    rotate(45f, pivot = Offset(cx, cy)) {
        for (row in 0..2) {
            for (col in 0..2) {
                val color = when {
                    row == 1 && col == 1 -> accent
                    (row + col) % 2 == 0 -> light
                    else -> dim
                }
                drawRoundRect(
                    color = color.copy(alpha = color.alpha * alpha),
                    topLeft = Offset(
                        cx + (col - 1) * step - tile / 2f,
                        cy + (row - 1) * step - tile / 2f
                    ),
                    size = Size(tile, tile),
                    cornerRadius = CornerRadius(tile * 0.2f)
                )
            }
        }
    }
}

/** Логотип целиком — знак, название и подпись. Используется на входе в приложение. */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 72.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RhombusMark(size = markSize)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ПЛАТФОРМА",
                style = MaterialTheme.typography.displaySmall,
                color = TileWhite,
                letterSpacing = 6.sp
            )
            Text(
                text = "спортивный клуб",
                style = MaterialTheme.typography.labelSmall,
                color = PlatformaRed,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 5.sp
            )
        }
    }
}

/** Маленький красный ромб — фирменный маркер списка вместо обычной точки. */
@Composable
fun RhombusBullet(
    modifier: Modifier = Modifier,
    size: Dp = 7.dp,
    color: Color = PlatformaRed
) {
    Canvas(modifier.size(size)) {
        val side = this.size.minDimension * 0.7f
        rotate(45f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(
                    (this.size.width - side) / 2f,
                    (this.size.height - side) / 2f
                ),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * 0.22f)
            )
        }
    }
}
