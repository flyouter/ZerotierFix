package net.kaaass.zerotierfix.features.moon

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
import net.kaaass.zerotierfix.data.repository.MoonOrbitRepository
import net.kaaass.zerotierfix.di.IoDispatcher
import net.kaaass.zerotierfix.model.dto.MoonOrbitDto
import net.kaaass.zerotierfix.ui.common.UiText
import net.kaaass.zerotierfix.util.FileUtil
import net.kaaass.zerotierfix.vpn.engine.VpnEngine

import org.apache.commons.io.FileUtils

import java.io.FileInputStream
import java.io.IOException

import javax.inject.Inject

/**
 * Moon 入轨页面的 ViewModel。
 *
 * 文件导入、缓存文件与数据库操作均在 IO 线程执行；入轨命令交由 [VpnEngine]。
 */
@HiltViewModel
class MoonOrbitViewModel @Inject constructor(
    private val moonOrbitRepository: MoonOrbitRepository,
    private val vpnEngine: VpnEngine,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoonOrbitUiState())

    /** 页面状态。 */
    val uiState: StateFlow<MoonOrbitUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MoonOrbitEffect>(extraBufferCapacity = 8)

    /** 一次性事件。 */
    val effects: SharedFlow<MoonOrbitEffect> = _effects.asSharedFlow()

    /**
     * 重新读取入轨列表。
     */
    fun refresh() {
        viewModelScope.launch {
            val moons = withContext(ioDispatcher) { moonOrbitRepository.getAll() }
            _uiState.update { it.copy(moons = moons, isLoading = false) }
        }
    }

    /**
     * 从文件导入 Moon。
     *
     * @param uri 用户选择的文件
     */
    fun importFromFile(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { readMoonFile(uri) }
            when (result) {
                is MoonFileResult.Success -> addMoon(result.worldId, result.worldId, true)
                MoonFileResult.WrongFormat -> {
                    FileUtil.clearTempFile(context)
                    _effects.emit(
                        MoonOrbitEffect.ShowToast(UiText.Resource(R.string.moon_wrong_file_format))
                    )
                }
                MoonFileResult.CannotOpen -> {
                    FileUtil.clearTempFile(context)
                    _effects.emit(
                        MoonOrbitEffect.ShowToast(UiText.Resource(R.string.cannot_open_moon))
                    )
                }
            }
        }
    }

    /**
     * 手工输入 Moon 地址与种子并添加。
     *
     * @param worldIdText Moon 地址（16 进制）
     * @param seedText Moon 种子（16 进制）
     */
    fun addOrbitFromInput(worldIdText: String, seedText: String) {
        val worldId = parseMoonAddress(worldIdText)
        if (worldId == null) {
            _effects.tryEmit(
                MoonOrbitEffect.ShowSnackbar(
                    UiText.Resource(R.string.moon_world_id_wrong_format)
                )
            )
            return
        }
        val seed = parseMoonAddress(seedText)
        if (seed == null) {
            _effects.tryEmit(
                MoonOrbitEffect.ShowSnackbar(UiText.Resource(R.string.moon_seed_wrong_format))
            )
            return
        }
        viewModelScope.launch { addMoon(worldId, seed, false) }
    }

    /**
     * 删除入轨信息及其缓存文件。
     *
     * @param moon 目标 Moon
     */
    fun remove(moon: MoonOrbitDto) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                moonOrbitRepository.remove(moon.moonWorldId, moon.moonSeed)
            }
            _effects.emit(
                MoonOrbitEffect.ShowSnackbar(UiText.Resource(R.string.moon_orbit_delete_success))
            )
            refresh()
        }
    }

    /**
     * 删除 Moon 缓存文件。
     *
     * @param moon 目标 Moon
     */
    fun deleteCacheFile(moon: MoonOrbitDto) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                moonOrbitRepository.deleteCacheFile(moon.moonWorldId)
            }
            _effects.emit(
                MoonOrbitEffect.ShowSnackbar(UiText.Resource(R.string.cached_moon_file_delete))
            )
            refresh()
        }
    }

    /**
     * 写入入轨信息并入轨。
     *
     * @param worldId Moon 地址
     * @param seed Moon 种子
     * @param fromFile 是否来自文件导入
     */
    private suspend fun addMoon(worldId: Long, seed: Long, fromFile: Boolean) {
        var storeFileFailed = false
        val added = withContext(ioDispatcher) {
            if (moonOrbitRepository.exists(worldId)) {
                FileUtil.clearTempFile(context)
                false
            } else {
                if (fromFile) {
                    if (!moonOrbitRepository.storeMoonFile(worldId, FileUtil.tempFile(context))) {
                        storeFileFailed = true
                    }
                    FileUtil.clearTempFile(context)
                }
                moonOrbitRepository.add(worldId, seed, fromFile)
                true
            }
        }
        if (!added) {
            _effects.emit(
                MoonOrbitEffect.ShowToast(UiText.Resource(R.string.moon_orbit_exist))
            )
            return
        }
        if (storeFileFailed) {
            _effects.emit(
                MoonOrbitEffect.ShowToast(UiText.Resource(R.string.cannot_open_moon))
            )
        }
        _effects.emit(
            MoonOrbitEffect.ShowSnackbar(UiText.Resource(R.string.moon_orbit_add_success))
        )
        val moon = withContext(ioDispatcher) {
            MoonOrbitDto(
                worldId,
                seed,
                fromFile,
                moonOrbitRepository.hasCacheFile(worldId)
            )
        }
        vpnEngine.orbit(listOf(moon))
        refresh()
    }

    /**
     * 将用户选择的文件复制到临时文件并校验。
     *
     * @param uri 文件 URI
     * @return 校验结果
     */
    private fun readMoonFile(uri: Uri): MoonFileResult {
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    return MoonFileResult.CannotOpen
                }
                FileUtils.copyInputStreamToFile(input, FileUtil.tempFile(context))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Cannot copy moon file", e)
            return MoonFileResult.CannotOpen
        }
        val worldId = checkTempMoonFile()
        return if (worldId < 0) MoonFileResult.WrongFormat else MoonFileResult.Success(worldId)
    }

    /**
     * 校验临时 Moon 文件的文件头与地址。
     *
     * @return Moon 地址；格式非法时返回 `-1`
     */
    private fun checkTempMoonFile(): Long {
        val temp = FileUtil.tempFile(context)
        val dataLen = MOON_FILE_HEADER.size + 8
        val buf = ByteArray(dataLen)
        try {
            FileInputStream(temp).use { input ->
                if (input.read(buf) != dataLen) {
                    return -1
                }
            }
        } catch (e: IOException) {
            return -1
        }
        if (buf[0] != MOON_FILE_HEADER[0]) {
            Log.i(TAG, "Moon file has a wrong header")
            return -1
        }
        var worldId = 0L
        for (i in MOON_FILE_HEADER.size until dataLen) {
            worldId = (worldId shl 8) + (buf[i].toLong() and 0xff)
        }
        return worldId
    }

    /**
     * 解析 16 进制 Moon 地址。
     *
     * @param text 输入文本
     * @return 地址；格式非法或超出范围时返回 `null`
     */
    private fun parseMoonAddress(text: String): Long? {
        val value = text.toLongOrNull(16) ?: return null
        return if (value in 0..MOON_ADDRESS_MAX) value else null
    }

    /**
     * 文件导入结果。
     */
    private sealed interface MoonFileResult {

        /** 校验成功。 */
        data class Success(val worldId: Long) : MoonFileResult

        /** 文件头格式错误。 */
        data object WrongFormat : MoonFileResult

        /** 无法读取文件。 */
        data object CannotOpen : MoonFileResult
    }

    companion object {
        private const val TAG = "MoonOrbitViewModel"
        private val MOON_FILE_HEADER = byteArrayOf(0x7f)
        private const val MOON_ADDRESS_MAX = 0xffffffffffL
    }
}
