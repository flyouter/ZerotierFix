package net.kaaass.zerotierfix.features.join

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地图片的二维码解码结果。
 */
sealed interface QrImageDecodeResult {

    /** 识别到二维码，[text] 为原始内容（可能为空串）。 */
    data class Decoded(val text: String) : QrImageDecodeResult

    /** 图片中没有二维码。 */
    data object NoQrCode : QrImageDecodeResult

    /** 图片无法读取或解码。 */
    data object Failed : QrImageDecodeResult
}

/** 解码图片时的最大边长（像素），兼顾识别率与内存。 */
internal const val QR_IMAGE_MAX_EDGE = 2048

/**
 * 从本地图片解码二维码文本。
 *
 * 使用 `ImageDecoder` 读取，自动应用 EXIF 方向，因此传给 zxing-cpp 的旋转角固定为 0。
 */
suspend fun decodeQrCodeFromUri(context: Context, uri: Uri): QrImageDecodeResult =
    withContext(Dispatchers.IO) {
        var decoded: Bitmap? = null
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // zxing-cpp 通过 AndroidBitmap_lockPixels 读取像素，硬件位图会失败
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                // 避免 HDR/广色域图片输出 RGBA_F16
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                decoder.setTargetSampleSize(
                    calculateSampleSize(info.size.width, info.size.height)
                )
            }

            val sourceBitmap = decoded ?: return@withContext QrImageDecodeResult.Failed
            val bitmap = sourceBitmap.toArgb8888()
                ?: return@withContext QrImageDecodeResult.Failed
            try {
                val result = newQrCodeReader().read(bitmap).firstOrNull()
                    ?: return@withContext QrImageDecodeResult.NoQrCode
                QrImageDecodeResult.Decoded(result.text.orEmpty())
            } finally {
                if (bitmap !== sourceBitmap) {
                    bitmap.recycle()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            QrImageDecodeResult.Failed
        } finally {
            decoded?.recycle()
        }
    }

/**
 * 计算 2 的幂采样倍数，使图片最长边不超过 [maxEdge]。
 */
internal fun calculateSampleSize(
    width: Int,
    height: Int,
    maxEdge: Int = QR_IMAGE_MAX_EDGE
): Int {
    if (width <= 0 || height <= 0 || maxEdge <= 0) {
        return 1
    }
    var sample = 1
    while (maxOf(width, height) / sample > maxEdge) {
        sample = sample shl 1
    }
    return sample
}

/**
 * 保证 zxing-cpp 能直接读取像素：输出 ARGB_8888 格式。
 */
private fun Bitmap.toArgb8888(): Bitmap? = runCatching {
    if (config == Bitmap.Config.ARGB_8888) {
        this
    } else {
        copy(Bitmap.Config.ARGB_8888, false)
    }
}.getOrNull()
