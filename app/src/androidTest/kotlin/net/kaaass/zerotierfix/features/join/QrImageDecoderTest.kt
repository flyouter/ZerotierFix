package net.kaaass.zerotierfix.features.join

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File

/**
 * 验证从本地图片（Uri）解码二维码。
 */
@RunWith(AndroidJUnit4::class)
class QrImageDecoderTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun decodesJoinQrCodeFromFileUri() {
        val file = copyAssetToCache("join_qr.png")

        val result = runBlocking { decodeQrCodeFromUri(context, Uri.fromFile(file)) }

        assertTrue(result is QrImageDecodeResult.Decoded)
        assertEquals(
            "https://joinzt.com/addnetwork?nwid=673017c788cb3f7d&v=1",
            (result as QrImageDecodeResult.Decoded).text
        )
    }

    @Test
    fun reportsNoQrCodeForBlankImage() {
        val file = File(context.cacheDir, "blank.png")
        Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }

        val result = runBlocking { decodeQrCodeFromUri(context, Uri.fromFile(file)) }

        assertEquals(QrImageDecodeResult.NoQrCode, result)
    }

    @Test
    fun reportsFailureForCorruptImage() {
        val file = File(context.cacheDir, "corrupt.jpg").apply {
            writeText("not an image")
        }

        val result = runBlocking { decodeQrCodeFromUri(context, Uri.fromFile(file)) }

        assertEquals(QrImageDecodeResult.Failed, result)
    }

    private fun copyAssetToCache(name: String): File {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val file = File(context.cacheDir, name)
        assets.open(name).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }
}
