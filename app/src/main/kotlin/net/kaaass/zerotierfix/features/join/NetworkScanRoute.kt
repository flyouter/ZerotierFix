package net.kaaass.zerotierfix.features.join

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

import kotlinx.coroutines.launch

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.util.NetworkIdScanResult

/**
 * 扫码页面的路由层，负责相机权限状态机。
 *
 * @param onBack 返回
 * @param onScanned 识别到有效网络 ID
 */
@Composable
fun NetworkScanRoute(
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var permissionGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<CameraScanError?>(null) }

    val cameraAvailable = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    fun requestPermission() {
        if (permissionGranted) {
            return
        }
        val currentActivity = activity ?: return
        // 系统已不再允许弹窗（永久拒绝）时，直接引导到应用设置
        if (hasRequested &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                currentActivity,
                Manifest.permission.CAMERA
            )
        ) {
            context.openAppSettings()
            return
        }
        hasRequested = true
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 首次进入扫码页自动申请相机权限
    LaunchedEffect(Unit) {
        if (!permissionGranted && !hasRequested) {
            requestPermission()
        }
    }

    // “仅此一次”在退到后台后会被系统回收，回前台必须重新检查
    LifecycleResumeEffect(Unit) {
        permissionGranted = context.hasCameraPermission()
        onPauseOrDispose { torchOn = false }
    }

    val invalidQrMessage = stringResource(R.string.scan_invalid_qr)
    val invalidIdMessage = stringResource(R.string.scan_invalid_network_id)

    NetworkScanScreen(
        permissionGranted = permissionGranted,
        cameraAvailable = cameraAvailable,
        cameraError = cameraError,
        torchOn = torchOn,
        hasFlash = hasFlash,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRequestPermission = ::requestPermission,
        onOpenSettings = { context.openAppSettings() },
        onTorchToggle = { torchOn = !torchOn },
        onTorchAvailabilityChanged = { hasFlash = it },
        onScanned = onScanned,
        onInvalidCode = { result ->
            val message = if (result is NetworkIdScanResult.InvalidNetworkId) {
                invalidIdMessage
            } else {
                invalidQrMessage
            }
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        },
        onCameraError = { cameraError = it }
    )
}

/**
 * 打开本应用的系统设置页。
 */
private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
