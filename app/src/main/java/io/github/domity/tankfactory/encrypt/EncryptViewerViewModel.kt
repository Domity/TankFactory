package io.github.domity.tankfactory.encrypt

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
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

class EncryptViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private var password: String = ""

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    private val _decryptedFileName = MutableStateFlow<String?>(null)
    val decryptedFileName: StateFlow<String?> = _decryptedFileName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var encryptedData: ByteArray? = null
    private var decryptedData: ByteArray? = null
    private val context: Context get() = getApplication()

    fun onScreenEntered() {
        _selectedFileName.value = null
        password = ""
        _isProcessing.value = false
        _isDone.value = false
        _decryptedFileName.value = null
        _errorMessage.value = null
        encryptedData = null
        decryptedData = null
    }

    fun setFileUri(uri: Uri) {
        _isDone.value = false
        _decryptedFileName.value = null
        _errorMessage.value = null
        encryptedData = null
        decryptedData = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        _selectedFileName.value = name
                        _decryptedFileName.value = if (name.endsWith(".tankfactory")) {
                            name.removeSuffix(".tankfactory")
                        } else { "decrypted_$name" }
                    }
                }
                encryptedData = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {}
        }
    }

    fun setPassword(pwd: String) {
        password = pwd
        _errorMessage.value = null
    }

    fun decrypt() {
        val data = encryptedData ?: return
        val pwd = password
        if (pwd.isEmpty()) return

        _isProcessing.value = true
        _isDone.value = false
        _errorMessage.value = null
        decryptedData = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = EncryptCoder.decrypt(data, pwd)
                if (result != null) {
                    decryptedData = result
                    _isDone.value = true
                } else {
                    _errorMessage.value = context.getString(R.string.encrypt_viewer_decrypt_failed)
                }
            } catch (_: Exception) {
                _errorMessage.value = context.getString(R.string.encrypt_viewer_decrypt_failed)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveDecryptedFile() {
        val data = decryptedData ?: return
        val outputName = _decryptedFileName.value ?: "decrypted_file"

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
        encryptedData = null
        decryptedData = null
    }
}