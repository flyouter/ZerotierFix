package net.kaaass.zerotierfix.features.join

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

import net.kaaass.zerotierfix.util.NetworkIdScanParser
import net.kaaass.zerotierfix.util.NetworkIdScanResult

import java.util.concurrent.Executors

import zxingcpp.BarcodeReader

/**
 * 相机启动失败的原因。
 */
enum class CameraScanError {

    /** 设备没有可用相机。 */
    NO_CAMERA,

    /** CameraX 初始化失败。 */
    INIT_FAILED,

    /** 相机用例绑定失败。 */
    BIND_FAILED
}

/**
 * CameraX 相机预览与二维码解码。
 *
 * 预览使用 `PreviewView`（SurfaceView 优先），兼容性优于 Compose Viewfinder。
 *
 * @param torchOn 是否开启手电筒
 * @param onTorchAvailabilityChanged 手电筒可用性回调
 * @param onScanned 识别到有效邀请码回调，参数为 16 位小写网络 ID
 * @param onInvalidCode 识别到无效邀请码回调
 * @param onCameraError 相机错误回调
 * @param modifier 修饰符
 */
@Composable
fun CameraBarcodePreview(
    torchOn: Boolean,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onScanned: (String) -> Unit,
    onInvalidCode: (NetworkIdScanResult) -> Unit,
    onCameraError: (CameraScanError) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnInvalidCode by rememberUpdatedState(onInvalidCode)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val currentOnTorchAvailabilityChanged by rememberUpdatedState(onTorchAvailabilityChanged)

    val analysisExecutor = remember {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "zxing-decode").apply { isDaemon = true }
        }
    }
    val resultChannel = remember { Channel<String>(Channel.CONFLATED) }
    val analyzer = remember {
        BarcodeAnalyzer { raw -> resultChannel.trySend(raw).isSuccess }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val preview = remember {
        Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .build()
    }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .build()
    }

    val cameraProvider by produceState<ProcessCameraProvider?>(initialValue = null) {
        value = runCatching {
            ProcessCameraProvider.awaitInstance(context.applicationContext)
        }.onFailure {
            currentOnCameraError(CameraScanError.INIT_FAILED)
        }.getOrNull()
    }

    DisposableEffect(cameraProvider, lifecycleOwner) {
        val provider = cameraProvider
        if (provider != null && context.hasCameraPermission()) {
            val selector = chooseCameraSelector(provider)
            if (selector == null) {
                currentOnCameraError(CameraScanError.NO_CAMERA)
            } else {
                preview.setSurfaceProvider(previewView.surfaceProvider)
                analysis.setAnalyzer(analysisExecutor, analyzer)
                runCatching {
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        analysis
                    )
                }.onFailure {
                    currentOnCameraError(CameraScanError.BIND_FAILED)
                }
            }
        }

        onDispose {
            analyzer.stopAnalyzing()
            analysis.clearAnalyzer()
            camera?.cameraControl?.enableTorch(false)
            camera = null
            preview.setSurfaceProvider(null)
            provider?.unbind(preview, analysis)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            resultChannel.close()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(camera) {
        currentOnTorchAvailabilityChanged(camera?.cameraInfo?.hasFlashUnit() == true)
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    LaunchedEffect(Unit) {
        for (raw in resultChannel) {
            when (val result = NetworkIdScanParser.parse(raw)) {
                is NetworkIdScanResult.Success -> {
                    analyzer.stopAnalyzing()
                    currentOnScanned(result.networkId)
                    return@LaunchedEffect
                }
                else -> {
                    analyzer.resumeAnalyzing()
                    currentOnInvalidCode(result)
                    delay(INVALID_CODE_COOLDOWN_MS)
                }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

/**
 * 选择后置相机，不可用时回退到前置。
 */
private fun chooseCameraSelector(provider: ProcessCameraProvider): CameraSelector? =
    runCatching {
        when {
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                CameraSelector.DEFAULT_BACK_CAMERA
            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                CameraSelector.DEFAULT_FRONT_CAMERA
            else -> null
        }
    }.getOrNull()

/**
 * 判断是否已授予相机权限。
 */
internal fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 在单线程上执行的二维码分析器。
 *
 * zxing-cpp 的 `read(ImageProxy)` 不会关闭图像，必须由调用方关闭。
 */
private class BarcodeAnalyzer(
    private val onRawResult: (String) -> Boolean
) : ImageAnalysis.Analyzer {

    private val reader by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BarcodeReader().apply {
            options = BarcodeReader.Options(
                formats = setOf(BarcodeReader.Format.QR_CODE),
                tryHarder = true,
                tryRotate = true,
                tryInvert = true,
                maxNumberOfSymbols = 1
            )
        }
    }

    @Volatile
    private var stopped = false

    @Volatile
    private var awaitingMainThread = false

    /** 停止分析，用于已识别到有效结果或 native 解码失败。 */
    fun stopAnalyzing() {
        stopped = true
    }

    /** 恢复分析，用于无效码冷却结束。 */
    fun resumeAnalyzing() {
        awaitingMainThread = false
    }

    override fun analyze(image: ImageProxy) {
        if (stopped || awaitingMainThread) {
            image.close()
            return
        }

        val text = try {
            image.use { proxy ->
                reader.read(proxy).firstOrNull()?.text
            }
        } catch (_: Throwable) {
            // native 库加载失败或解码异常时停止分析，避免线程反复崩溃
            stopAnalyzing()
            null
        }

        if (text.isNullOrBlank()) {
            return
        }

        awaitingMainThread = true
        if (!onRawResult(text)) {
            awaitingMainThread = false
        }
    }
}

/** 无效二维码的提示冷却时间。 */
private const val INVALID_CODE_COOLDOWN_MS = 1_200L
