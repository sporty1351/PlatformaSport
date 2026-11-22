package ru.platformasport.club.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/**
 * Кодирует номер карты в изображение для сканера турникета.
 * Модули рисуются чёрным по белому — так их читает любой ридер.
 */
fun encodeBarcode(
    content: String,
    format: BarcodeFormat,
    width: Int,
    height: Int
): ImageBitmap? = runCatching {
    val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, format, width, height, hints)
    val w = matrix.width
    val h = matrix.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (matrix[x, y]) BLACK else WHITE
        }
    }
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        .apply { setPixels(pixels, 0, w, 0, 0, w, h) }
        .asImageBitmap()
}.getOrNull()

@Composable
fun rememberBarcode(content: String, width: Int = 900, height: Int = 260): ImageBitmap? =
    remember(content, width, height) {
        encodeBarcode(content, BarcodeFormat.CODE_128, width, height)
    }

@Composable
fun rememberQrCode(content: String, size: Int = 640): ImageBitmap? =
    remember(content, size) {
        encodeBarcode(content, BarcodeFormat.QR_CODE, size, size)
    }

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
