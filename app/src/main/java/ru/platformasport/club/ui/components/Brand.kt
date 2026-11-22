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
 * Знак клуба: девять плиток в ромбической проекции,
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
    // Девять плиток в перспективе, как в фирменном знаке клуба.
    // Координаты заданы в общей системе: весь силуэт помещается в Canvas.
    val unit = minOf(size.width / 6.6f, size.height / 4.7f)
    val cx = center.x + centerOffset.x
    val cy = center.y - unit * 0.25f + centerOffset.y
    for (row in 0..2) for (col in 0..2) {

        val x = cx + (col - row) * unit
        val y = cy + (col + row - 2) * unit * 0.70f
        val color = when {
            row == 1 && col == 1 -> accent
            (row + col) % 2 == 0 -> light
            else -> dim
        }
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(x, y - unit * 0.59f)
            lineTo(x + unit * 0.86f, y)
            lineTo(x, y + unit * 0.59f)
            lineTo(x - unit * 0.86f, y)
            close()
        }
        drawPath(path, color.copy(alpha = color.alpha * alpha))
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
