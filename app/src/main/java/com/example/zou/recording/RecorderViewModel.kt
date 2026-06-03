package com.example.zou.recording

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.zou.recording.audio.AudioPlayer
import com.example.zou.recording.audio.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * 狀態層:以 [StateFlow] 對外發布單一 [RecorderUiState],並協調
 * [AudioRecorder] 與 [AudioPlayer]。UI 層只觀察 [uiState] 並呼叫以下方法。
 */
class RecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    /** 目前可播放的音檔(自錄或選檔)。 */
    private var currentUri: Uri? = null

    /** 我們自己錄出來的檔,供刪除;選檔來源不在此追蹤。 */
    private var recordedFile: File? = null

    private val recorder = AudioRecorder(application)
    private val player = AudioPlayer(application)

    init {
        val granted = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasPermission = granted) }
    }

    fun onFileNameChange(name: String) {
        _uiState.update { it.copy(fileName = name) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasPermission = granted,
                message = if (granted) "已取得錄音權限" else "未取得錄音權限"
            )
        }
    }

    fun startRecording() {
        if (!_uiState.value.hasPermission) {
            _uiState.update { it.copy(message = "請先按『權限』授權錄音") }
            return
        }

        val dir = getApplication<Application>()
            .getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        dir?.let { if (!it.exists()) it.mkdirs() }

        val rawName = _uiState.value.fileName.trim()
        val name = rawName.ifBlank { "recording" }
        val file = File(dir, "$name.m4a")

        try {
            recorder.start(file)
            recordedFile = file
            currentUri = Uri.fromFile(file)
            _uiState.update {
                it.copy(
                    phase = RecorderPhase.RECORDING,
                    displayName = file.name,
                    displayPath = file.absolutePath,
                    message = "開始錄音"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = RecorderPhase.IDLE,
                    message = "錄音發生錯誤"
                )
            }
        }
    }

    fun stopRecording() {
        recorder.stop()
        _uiState.update {
            it.copy(
                phase = RecorderPhase.READY,
                message = "結束錄音"
            )
        }
    }

    fun togglePlayPause() {
        try {
            when (_uiState.value.phase) {
                RecorderPhase.READY -> {
                    val uri = currentUri ?: return
                    player.play(uri) {
                        _uiState.update { it.copy(phase = RecorderPhase.READY) }
                    }
                    _uiState.update { it.copy(phase = RecorderPhase.PLAYING) }
                }

                RecorderPhase.PLAYING -> {
                    player.pause()
                    _uiState.update { it.copy(phase = RecorderPhase.PAUSED) }
                }

                RecorderPhase.PAUSED -> {
                    player.resume()
                    _uiState.update { it.copy(phase = RecorderPhase.PLAYING) }
                }

                else -> { /* IDLE / RECORDING:忽略 */ }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(message = "播放發生錯誤") }
        }
    }

    fun stopPlaying() {
        player.stop()
        _uiState.update { it.copy(phase = RecorderPhase.READY) }
    }

    fun deleteCurrent() {
        player.stop()
        recordedFile?.delete()
        currentUri = null
        recordedFile = null
        _uiState.update {
            it.copy(
                phase = RecorderPhase.IDLE,
                displayName = "",
                displayPath = "",
                message = "已刪除檔案"
            )
        }
    }

    fun onFilePicked(uri: Uri) {
        currentUri = uri
        recordedFile = null
        _uiState.update {
            it.copy(
                phase = RecorderPhase.READY,
                displayName = uri.lastPathSegment ?: uri.toString(),
                displayPath = uri.toString(),
                message = "已選取檔案"
            )
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.stop()
        player.release()
    }
}
