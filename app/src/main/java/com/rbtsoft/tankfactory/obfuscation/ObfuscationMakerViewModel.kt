package com.rbtsoft.tankfactory.obfuscation

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
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

class ObfuscationMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone

    private var encryptedData: ByteArray? = null
    private var originalData: ByteArray? = null

    fun onScreenEntered() {
        _selectedFileName.value = null
        _password.value = ""
        _isProcessing.value = false
        _isDone.value = false
        encryptedData = null
        originalData = null
    }

    fun setFileUri(uri: Uri) {
        _isDone.value = false
        _password.value = ""
        encryptedData = null
        originalData = null

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            try {
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            _selectedFileName.value = cursor.getString(nameIdx)
                        }
                    }
                }

                originalData = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {}
        }
    }

    fun encrypt() {
        val data = originalData ?: return

        _isProcessing.value = true
        _isDone.value = false
        _password.value = ""
        encryptedData = null

        viewModelScope.launch(Dispatchers.Default) {
            val app = getApplication<Application>()
            try {
                val pwd = ObfuscationCoder.generatePassword()
                val result = ObfuscationCoder.encrypt(data, pwd)
                encryptedData = result
                _password.value = pwd
                _isDone.value = true
                originalData = null
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(app, e.message ?: app.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun copyPasswordToClipboard() {
        val pwd = _password.value
        if (pwd.isEmpty()) return

        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("tank_password", pwd))
        Toast.makeText(app, app.getString(R.string.obfuscation_maker_password_copied), Toast.LENGTH_SHORT).show()
    }

    fun saveEncryptedFile() {
        val data = encryptedData ?: return
        val app = getApplication<Application>()
        val originalName = _selectedFileName.value ?: "image"
        val outputName = "${originalName}.tankfactory"

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
        originalData = null
    }
}
