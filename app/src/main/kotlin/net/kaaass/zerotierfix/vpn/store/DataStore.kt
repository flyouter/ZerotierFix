package net.kaaass.zerotierfix.vpn.store

import android.content.Context
import android.util.Log

import com.zerotier.sdk.DataStoreGetListener
import com.zerotier.sdk.DataStorePutListener

import net.kaaass.zerotierfix.data.repository.SettingsRepository
import net.kaaass.zerotierfix.util.Constants

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Zerotier 文件数据源。
 */
class DataStore(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : DataStoreGetListener, DataStorePutListener {

    override fun onDataStorePut(name: String, buffer: ByteArray, secure: Boolean): Int {
        Log.d(TAG, "Writing File: " + name + ", to: " + this.context.filesDir)
        // 保护自定义 Planet 文件
        if (hookPlanetFile(name)) {
            return 0
        }
        try {
            if (name.contains("/")) {
                val file = File(
                    this.context.filesDir,
                    name.substring(0, name.lastIndexOf('/'))
                )
                if (!file.exists()) {
                    file.mkdirs()
                }
                val fileOutputStream = FileOutputStream(
                    File(file, name.substring(name.lastIndexOf('/') + 1))
                )
                fileOutputStream.write(buffer)
                fileOutputStream.flush()
                fileOutputStream.close()
                return 0
            }
            val openFileOutput = this.context.openFileOutput(name, 0)
            openFileOutput.write(buffer)
            openFileOutput.flush()
            openFileOutput.close()
            return 0
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return -1
        } catch (e2: IOException) {
            val stringWriter = StringWriter()
            e2.printStackTrace(PrintWriter(stringWriter))
            Log.e(TAG, stringWriter.toString())
            return -2
        } catch (e3: IllegalArgumentException) {
            val stringWriter2 = StringWriter()
            e3.printStackTrace(PrintWriter(stringWriter2))
            Log.e(TAG, stringWriter2.toString())
            return -3
        }
    }

    override fun onDelete(name: String): Int {
        Log.d(TAG, "Deleting File: $name")
        // 保护自定义 Planet 文件
        if (hookPlanetFile(name)) {
            return 0
        }
        val deleted: Boolean = if (name.contains("/")) {
            val file = File(this.context.filesDir, name)
            if (!file.exists()) {
                true
            } else {
                file.delete()
            }
        } else {
            this.context.deleteFile(name)
        }
        return if (!deleted) 1 else 0
    }

    override fun onDataStoreGet(name: String, out_buffer: ByteArray): Long {
        var fileName = name
        Log.d(TAG, "Reading File: $fileName")
        if (hookPlanetFile(fileName)) {
            fileName = Constants.FILE_CUSTOM_PLANET
        }
        // 读入文件
        try {
            if (fileName.contains("/")) {
                val file = File(
                    this.context.filesDir,
                    fileName.substring(0, fileName.lastIndexOf('/'))
                )
                if (!file.exists()) {
                    file.mkdirs()
                }
                val file2 = File(file, fileName.substring(fileName.lastIndexOf('/') + 1))
                if (!file2.exists()) {
                    return 0
                }
                val fileInputStream = FileInputStream(file2)
                val read = fileInputStream.read(out_buffer)
                fileInputStream.close()
                return read.toLong()
            }
            val openFileInput = this.context.openFileInput(fileName)
            val read2 = openFileInput.read(out_buffer)
            openFileInput.close()
            return read2.toLong()
        } catch (unused: FileNotFoundException) {
            return -1
        } catch (e: IOException) {
            Log.e(TAG, "", e)
            return -2
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            return -3
        }
    }

    /**
     * 判断是否为自定义 Planet 文件。
     *
     * @param name 文件名
     * @return 需要保护时返回 `true`
     */
    internal fun hookPlanetFile(name: String): Boolean {
        if (Constants.FILE_PLANET == name) {
            return this.settingsRepository.isPlanetUseCustom
        }
        return false
    }

    companion object {
        private const val TAG = "DataStore"
    }
}
