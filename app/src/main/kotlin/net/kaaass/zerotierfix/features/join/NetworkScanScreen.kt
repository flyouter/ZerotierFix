package net.kaaass.zerotierfix.features.join

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.ui.common.ZerotierTopBar
import net.kaaass.zerotierfix.ui.theme.ZerotierFixTheme
import net.kaaass.zerotierfix.util.NetworkIdScanResult

import kotlin.math.min

/**
 * 扫码页面。
 *
 * @param permissionGranted 是否已授予相机权限
 * @param cameraAvailable 设备是否有可用相机
 * @param cameraError 相机错误
 * @param torchOn 手电筒是否开启
 * @param hasFlash 相机是否有闪光灯
 * @param imageDecoding 是否正在解码本地图片
 * @param snackbarHostState Snackbar 宿主状态
 * @param onBack 返回
 * @param onRequestPermission 申请相机权限
 * @param onOpenSettings 打开应用设置
 * @param onTorchToggle 切换手电筒
 * @param onTorchAvailabilityChanged 手电筒可用性变化
 * @param onPickImage 从本地图片选择二维码
 * @param onScanned 识别成功
 * @param onInvalidCode 识别到无效邀请码
 * @param onCameraError 相机错误回调
 * @param modifier 修饰符
 */
@Composable
fun NetworkScanScreen(
    permissionGranted: Boolean,
    cameraAvailable: Boolean,
    cameraError: CameraScanError?,
    torchOn: Boolean,
    hasFlash: Boolean,
    imageDecoding: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTorchToggle: () -> Unit,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onScanned: (String) -> Unit,
    onInvalidCode: (NetworkIdScanResult) -> Unit,
    onCameraError: (CameraScanError) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewVisible = permissionGranted && cameraAvailable && cameraError == null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ZerotierTopBar(
                title = stringResource(R.string.scan_network_title),
                onBack = onBack,
                actions = {
                    if (previewVisible && hasFlash) {
                        IconButton(onClick = onTorchToggle) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flash_on),
                                contentDescription = stringResource(R.string.cd_toggle_torch),
                                tint = if (torchOn) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    // 相册入口不受相机权限/相机错误影响
                    IconButton(
                        onClick = onPickImage,
                        enabled = !imageDecoding
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_photo_library),
                            contentDescription = stringResource(R.string.cd_pick_image)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                !permissionGranted -> PermissionGate(
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
                !cameraAvailable -> CameraErrorState(
                    message = stringResource(R.string.scan_camera_unavailable),
                    onBack = onBack
                )
                cameraError != null -> CameraErrorState(
                    message = stringResource(R.string.scan_camera_error),
                    onBack = onBack
                )
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        CameraBarcodePreview(
                            torchOn = torchOn,
                            paused = imageDecoding,
                            onTorchAvailabilityChanged = onTorchAvailabilityChanged,
                            onScanned = onScanned,
                            onInvalidCode = onInvalidCode,
                            onCameraError = onCameraError,
                            modifier = Modifier.fillMaxSize()
                        )
                        ScanOverlay(modifier = Modifier.fillMaxSize())
                        Text(
                            text = stringResource(R.string.scan_hint),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 32.dp, vertical = 24.dp)
                                .background(
                                    color = Color.Black.copy(alpha = HINT_SCRIM_ALPHA),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (imageDecoding) {
                ImageDecodingOverlay(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * 正在解码本地图片时的遮罩。
 */
@Composable
private fun ImageDecodingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = DECODING_SCRIM_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = stringResource(R.string.scan_image_decoding),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * 权限门禁。
 */
@Composable
private fun PermissionGate(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_qr_code_scanner),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(R.string.scan_permission_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = stringResource(R.string.scan_permission_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(stringResource(R.string.scan_permission_grant))
        }
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.scan_open_settings))
        }
    }
}

/**
 * 相机不可用或启动失败时的占位。
 */
@Composable
private fun CameraErrorState(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(stringResource(R.string.cd_back))
        }
    }
}

/**
 * 取景遮罩：四周半透明，中央留出方形取景区并绘制四角标记。
 */
@Composable
private fun ScanOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = min(size.width * FRAME_WIDTH_RATIO, size.height * FRAME_HEIGHT_RATIO)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val right = left + side
        val bottom = top + side
        val mask = Color.Black.copy(alpha = MASK_ALPHA)

        drawRect(mask, size = Size(size.width, top))
        drawRect(
            color = mask,
            topLeft = Offset(0f, bottom),
            size = Size(size.width, size.height - bottom)
        )
        drawRect(
            color = mask,
            topLeft = Offset(0f, top),
            size = Size(left, side)
        )
        drawRect(
            color = mask,
            topLeft = Offset(right, top),
            size = Size(size.width - right, side)
        )

        val corner = side * CORNER_RATIO
        val stroke = CORNER_STROKE_DP.dp.toPx()
        val corners = listOf(
            Offset(left, top) to Offset(1f, 1f),
            Offset(right, top) to Offset(-1f, 1f),
            Offset(left, bottom) to Offset(1f, -1f),
            Offset(right, bottom) to Offset(-1f, -1f)
        )
        for ((origin, direction) in corners) {
            drawLine(
                color = Color.White,
                start = origin,
                end = Offset(origin.x + direction.x * corner, origin.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = origin,
                end = Offset(origin.x, origin.y + direction.y * corner),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/** 取景区边长占可用宽度的比例。 */
private const val FRAME_WIDTH_RATIO = 0.72f

/** 取景区边长占可用高度的比例，横屏时避免压住底部提示。 */
private const val FRAME_HEIGHT_RATIO = 0.5f

/** 遮罩透明度。 */
private const val MASK_ALPHA = 0.55f

/** 角标长度占取景区边长的比例。 */
private const val CORNER_RATIO = 0.14f

/** 角标线宽。 */
private const val CORNER_STROKE_DP = 4

/** 底部提示底衬透明度。 */
private const val HINT_SCRIM_ALPHA = 0.55f

/** 图片解码遮罩透明度。 */
private const val DECODING_SCRIM_ALPHA = 0.6f

@Preview(showBackground = true)
@Composable
private fun NetworkScanScreenPermissionPreview() {
    ZerotierFixTheme {
        NetworkScanScreen(
            permissionGranted = false,
            cameraAvailable = true,
            cameraError = null,
            torchOn = false,
            hasFlash = false,
            imageDecoding = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onTorchToggle = {},
            onTorchAvailabilityChanged = {},
            onPickImage = {},
            onScanned = {},
            onInvalidCode = {},
            onCameraError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkScanScreenImageDecodingPreview() {
    ZerotierFixTheme {
        NetworkScanScreen(
            permissionGranted = false,
            cameraAvailable = true,
            cameraError = null,
            torchOn = false,
            hasFlash = false,
            imageDecoding = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onTorchToggle = {},
            onTorchAvailabilityChanged = {},
            onPickImage = {},
            onScanned = {},
            onInvalidCode = {},
            onCameraError = {}
        )
    }
}
