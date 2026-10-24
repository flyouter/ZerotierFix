package net.kaaass.zerotierfix.features.join

import zxingcpp.BarcodeReader

/**
 * 创建二维码解码器。
 *
 * 每次调用返回新实例：`BarcodeReader` 不是线程安全的，相机分析线程与图片解码需各自持有。
 */
internal fun newQrCodeReader(): BarcodeReader = BarcodeReader().apply {
    options = BarcodeReader.Options(
        formats = setOf(BarcodeReader.Format.QR_CODE),
        tryHarder = true,
        tryRotate = true,
        tryInvert = true,
        maxNumberOfSymbols = 1
    )
}
