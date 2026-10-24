package net.kaaass.zerotierfix.features.join

import android.graphics.BitmapFactory

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import net.kaaass.zerotierfix.util.NetworkIdScanParser
import net.kaaass.zerotierfix.util.NetworkIdScanResult

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import zxingcpp.BarcodeReader

/**
 * 验证 zxing-cpp 原生库能在设备上解码网络邀请二维码。
 */
@RunWith(AndroidJUnit4::class)
class BarcodeDecoderTest {

    @Test
    fun decodeJoinQrCode() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open("join_qr.png").use { BitmapFactory.decodeStream(it) }

        val reader = BarcodeReader().apply {
            options = BarcodeReader.Options(
                formats = setOf(BarcodeReader.Format.QR_CODE),
                tryHarder = true
            )
        }
        val results = reader.read(bitmap)

        assertEquals(1, results.size)
        val text = results.first().text
        assertEquals(
            "https://joinzt.com/addnetwork?nwid=673017c788cb3f7d&v=1",
            text
        )
        assertEquals(
            NetworkIdScanResult.Success("673017c788cb3f7d"),
            NetworkIdScanParser.parse(text!!)
        )
    }
}
