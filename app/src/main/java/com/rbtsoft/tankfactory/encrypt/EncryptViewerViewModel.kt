package com.rbtsoft.tankfactory.encrypt

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rbtsoft.tankfactory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EncryptViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName

    private var password: String = ""

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone

    private val _decryptedFileName = MutableStateFlow<String?>(null)
    val decryptedFileName: StateFlow<String?> = _decryptedFileName

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var encryptedData: ByteArray? = null
    private var decryptedData: ByteArray? = null

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
            val app = getApplication<Application>()
            try {
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            val name = cursor.getString(nameIdx)
                            _selectedFileName.value = name
                            _decryptedFileName.value = if (name.endsWith(".tankfactory")) {
                                name.removeSuffix(".tankfactory")
                            } else {
                                "decrypted_$name"
                            }
                        }
                    }
                }

                encryptedData = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
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
            val app = getApplication<Application>()
            try {
                val result = EncryptCoder.decrypt(data, pwd)
                if (result != null) {
                    decryptedData = result
                    _isDone.value = true
                } else {
                    decryptedData = null
                    _errorMessage.value = app.getString(R.string.encrypt_viewer_decrypt_failed)
                }
            } catch (_: Exception) {
                _errorMessage.value = app.getString(R.string.encrypt_viewer_decrypt_failed)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveDecryptedFile() {
        val data = decryptedData ?: return
        val app = getApplication<Application>()
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
                app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    app.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(data)
                        success = true
                    }
                }
            } catch (_: Exception) { }

            withContext(Dispatchers.Main) {
                _isProcessing.value = false
                if (success) {
                    Toast.makeText(app, app.getString(R.string.save_success), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(app, app.getString(R.string.save_failed, ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        encryptedData = null
        decryptedData = null
    }
}
