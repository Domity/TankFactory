package io.github.domity.tankfactory.encrypt

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.domity.tankfactory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Arrays

class EncryptMakerViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private var generatedPassword: CharArray? = null

    private val _isPasswordAvailable = MutableStateFlow(false)
    val isPasswordAvailable: StateFlow<Boolean> = _isPasswordAvailable.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    private var encryptedData: ByteArray? = null
    private var originalData: ByteArray? = null

    private val context: Context get() = getApplication()

    private fun ByteArray?.secureClear(): ByteArray? {
        if (this != null) Arrays.fill(this, 0.toByte())
        return null
    }

    private fun CharArray?.secureClear(): CharArray? {
        if (this != null) Arrays.fill(this, '\u0000')
        return null
    }

    fun onScreenEntered() {
        _selectedFileName.value = null
        generatedPassword = generatedPassword.secureClear()
        _isPasswordAvailable.value = false
        _isProcessing.value = false
        _isDone.value = false
        encryptedData = encryptedData.secureClear()
        originalData = originalData.secureClear()
    }

    fun setFileUri(uri: Uri) {
        _isDone.value = false
        generatedPassword = generatedPassword.secureClear()
        _isPasswordAvailable.value = false
        encryptedData = encryptedData.secureClear()
        originalData = originalData.secureClear()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) { _selectedFileName.value = cursor.getString(0) }
                }
                originalData = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    Toast.makeText(context, R.string.save_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun encrypt() {
        val data = originalData ?: return

        _isProcessing.value = true
        _isDone.value = false
        generatedPassword = generatedPassword.secureClear()
        _isPasswordAvailable.value = false
        encryptedData = encryptedData.secureClear()

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pwd = EncryptCoder.generatePassword()
                encryptedData = EncryptCoder.encrypt(data, pwd)
                generatedPassword = pwd
                _isPasswordAvailable.value = true
                _isDone.value = true
                originalData = originalData.secureClear()
            } catch (e: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    Toast.makeText(context, e.message ?: context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                }
            } finally { _isProcessing.value = false }
        }
    }

    fun copyPasswordToClipboard() {
        val pwd = generatedPassword ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("tank_password", String(pwd)).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        clipboard.setPrimaryClip(clipData)
        Toast.makeText(context, context.getString(R.string.encrypt_maker_password_copied), Toast.LENGTH_SHORT).show()
    }

    fun saveEncryptedFile() {
        val data = encryptedData ?: return
        val outputName = "${_selectedFileName.value ?: "image"}.tankfactory"

        _isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, outputName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(data)
                        success = true
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main.immediate) {
                _isProcessing.value = false
                val msgRes = if (success) R.string.save_success else R.string.save_failed
                Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        generatedPassword = generatedPassword.secureClear()
        encryptedData = encryptedData.secureClear()
        originalData = originalData.secureClear()
    }
}