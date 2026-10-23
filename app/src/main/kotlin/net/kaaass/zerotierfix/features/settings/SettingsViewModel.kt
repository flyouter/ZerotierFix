package net.kaaass.zerotierfix.features.settings

import android.content.Context
import android.net.Uri
import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import net.kaaass.zerotierfix.R
import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.ui.common.UiText
import net.kaaass.zerotierfix.util.Constants
import net.kaaass.zerotierfix.util.FileUtil

import org.apache.commons.io.FileUtils

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL

import javax.inject.Inject

/**
 * 设置页面的 ViewModel。
 *
 * Planet 文件校验规则与原实现一致：文件首字节必须为 `0x01`；
 * 文件来源支持本地文件与 URL 下载。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** 页面状态。 */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 8)

    /** 一次性事件。 */
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        isCellularDataAllowed = settings.isCellularDataAllowed,
                        isIpv6Disabled = settings.isIpv6Disabled,
                        isPlanetUseCustom = settings.isPlanetUseCustom
                    )
                }
            }
        }
    }

    /**
     * 切换自定义 planet。
     *
     * 打开时若本地还没有 planet 文件，弹出文件来源选择框。
     *
     * @param enabled 是否启用
     */
    fun setPlanetUseCustom(enabled: Boolean) {
        settingsRepository.setPlanetUseCustom(enabled)
        if (enabled && !customPlanetFileExists()) {
            _uiState.update { it.copy(isPlanetDialogVisible = true) }
        }
    }

    /**
     * 显示 planet 文件来源对话框。
     */
    fun openPlanetDialog() {
        _uiState.update { it.copy(isPlanetDialogVisible = true) }
    }

    /**
     * 关闭 planet 文件来源对话框。
     *
     * 若本地仍没有 planet 文件，则取消“使用自定义 planet”选项。
     */
    fun dismissPlanetDialog() {
        _uiState.update { it.copy(isPlanetDialogVisible = false, isUrlDialogVisible = false) }
        if (!customPlanetFileExists()) {
            settingsRepository.setPlanetUseCustom(false)
        }
    }

    /**
     * 显示 URL 输入对话框。
     */
    fun openUrlDialog() {
        _uiState.update { it.copy(isUrlDialogVisible = true) }
    }

    /**
     * 关闭 URL 输入对话框。
     */
    fun dismissUrlDialog() {
        _uiState.update { it.copy(isUrlDialogVisible = false) }
    }

    /**
     * 更新 URL 输入值。
     *
     * @param value 输入文本
     */
    fun onUrlChanged(value: String) {
        _uiState.update { it.copy(urlText = value) }
    }

    /**
     * 切换是否允许移动数据。
     *
     * @param value 是否允许
     */
    fun setCellularDataAllowed(value: Boolean) {
        settingsRepository.setCellularDataAllowed(value)
    }

    /**
     * 切换是否禁用 IPv6。
     *
     * @param value 是否禁用
     */
    fun setIpv6Disabled(value: Boolean) {
        settingsRepository.setIpv6Disabled(value)
    }

    /**
     * 从本地文件设置 planet。
     *
     * @param uri 用户选择的文件
     */
    fun setPlanetFromFile(uri: Uri) {
        viewModelScope.launch {
            val outcome = withContext(ioDispatcher) {
                try {
                    context.contentResolver.openInputStream(uri).use { input ->
                        if (input == null) {
                            return@withContext PlanetOutcome.CannotOpen
                        }
                        FileUtils.copyInputStreamToFile(input, FileUtil.tempFile(context))
                    }
                    if (dealTempPlanetFile()) {
                        PlanetOutcome.Success
                    } else {
                        PlanetOutcome.WrongFormat
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Cannot copy planet file", e)
                    PlanetOutcome.CannotOpen
                } finally {
                    FileUtil.clearTempFile(context)
                }
            }
            handlePlanetOutcome(outcome)
        }
    }

    /**
     * 从 URL 下载并设置 planet。
     *
     * @param urlText URL 文本
     */
    fun setPlanetFromUrl(urlText: String) {
        val url = try {
            URL(urlText)
        } catch (e: MalformedURLException) {
            _effects.tryEmit(
                SettingsEffect.ShowToast(UiText.Resource(R.string.wrong_url_format))
            )
            return
        }
        _uiState.update {
            it.copy(
                isUrlDialogVisible = false,
                isPlanetDialogVisible = false,
                isLoadingDialogVisible = true
            )
        }
        viewModelScope.launch {
            val outcome = withContext(ioDispatcher) {
                try {
                    FileUtils.copyURLToFile(
                        url,
                        FileUtil.tempFile(context),
                        PLANET_DOWNLOAD_CONN_TIMEOUT,
                        PLANET_DOWNLOAD_TIMEOUT
                    )
                    if (dealTempPlanetFile()) {
                        PlanetOutcome.Success
                    } else {
                        PlanetOutcome.WrongFormat
                    }
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG, "Timeout when downloading planet file", e)
                    PlanetOutcome.Timeout
                } catch (e: IOException) {
                    Log.e(TAG, "Cannot download planet file", e)
                    PlanetOutcome.CannotDownload
                } finally {
                    FileUtil.clearTempFile(context)
                }
            }
            _uiState.update { it.copy(isLoadingDialogVisible = false) }
            handlePlanetOutcome(outcome)
        }
    }

    /**
     * 处理 planet 设置结果。
     *
     * @param outcome 结果
     */
    private suspend fun handlePlanetOutcome(outcome: PlanetOutcome) {
        when (outcome) {
            PlanetOutcome.Success ->
                _effects.emit(
                    SettingsEffect.ShowSnackbar(UiText.Resource(R.string.set_planet_succ))
                )
            PlanetOutcome.WrongFormat ->
                _effects.emit(
                    SettingsEffect.ShowToast(UiText.Resource(R.string.planet_wrong_format))
                )
            PlanetOutcome.CannotOpen ->
                _effects.emit(
                    SettingsEffect.ShowToast(UiText.Resource(R.string.cannot_open_planet))
                )
            PlanetOutcome.CannotDownload ->
                _effects.emit(
                    SettingsEffect.ShowToast(
                        UiText.Resource(R.string.cannot_download_planet_file)
                    )
                )
            PlanetOutcome.Timeout ->
                _effects.emit(
                    SettingsEffect.ShowToast(UiText.Resource(R.string.planet_download_timeout))
                )
        }
        dismissPlanetDialog()
    }

    /**
     * 将临时文件校验并移动为自定义 planet 文件。
     *
     * @return 成功返回 `true`
     */
    private fun dealTempPlanetFile(): Boolean {
        val temp = FileUtil.tempFile(context)
        val buffer = ByteArray(PLANET_FILE_HEADER.size)
        try {
            FileInputStream(temp).use { input ->
                if (input.read(buffer) != PLANET_FILE_HEADER.size) {
                    return false
                }
            }
        } catch (e: IOException) {
            return false
        }
        if (!buffer.contentEquals(PLANET_FILE_HEADER)) {
            Log.i(TAG, "Planet file has a wrong header")
            return false
        }
        val destination = File(context.filesDir, Constants.FILE_CUSTOM_PLANET)
        return temp.renameTo(destination)
    }

    /**
     * 判断自定义 planet 文件是否存在。
     */
    private fun customPlanetFileExists(): Boolean =
        File(context.filesDir, Constants.FILE_CUSTOM_PLANET).exists()

    /**
     * Planet 设置结果。
     */
    private enum class PlanetOutcome {
        /** 设置成功。 */
        Success,

        /** 文件格式错误。 */
        WrongFormat,

        /** 无法读取文件。 */
        CannotOpen,

        /** 下载失败。 */
        CannotDownload,

        /** 下载超时。 */
        Timeout
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PLANET_DOWNLOAD_CONN_TIMEOUT = 5 * 1000
        private const val PLANET_DOWNLOAD_TIMEOUT = 10 * 1000
        private val PLANET_FILE_HEADER = byteArrayOf(0x01)
    }
}
