package com.example.zou.recording

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zou.recording.audio.AudioPlayer
import com.example.zou.recording.audio.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    /** 錄音計時 + 波形取樣輪詢。 */
    private var recordJob: Job? = null

    /** 播放進度輪詢。 */
    private var playJob: Job? = null

    private companion object {
        const val MAX_AMPLITUDE = 32767f
        const val AMPLITUDE_WINDOW = 60
        const val RECORD_POLL_MS = 100L
        const val PLAY_POLL_MS = 200L
    }

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
                    elapsedMs = 0L,
                    amplitudes = emptyList(),
                    message = "開始錄音"
                )
            }
            startRecordPolling()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = RecorderPhase.IDLE,
                    message = "錄音發生錯誤"
                )
            }
        }
    }

    /**
     * 錄音中每約 [RECORD_POLL_MS] 取樣一次:更新經過時間與滾動波形視窗。
     */
    private fun startRecordPolling() {
        recordJob?.cancel()
        val startAt = SystemClock.elapsedRealtime()
        recordJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startAt
                val level = (recorder.maxAmplitude() / MAX_AMPLITUDE)
                    .coerceIn(0f, 1f)
                _uiState.update { state ->
                    val window = (state.amplitudes + level)
                        .takeLast(AMPLITUDE_WINDOW)
                    state.copy(elapsedMs = elapsed, amplitudes = window)
                }
                delay(RECORD_POLL_MS)
            }
        }
    }

    fun stopRecording() {
        recorder.stop()
        recordJob?.cancel()
        recordJob = null
        _uiState.update {
            it.copy(
                phase = RecorderPhase.READY,
                elapsedMs = 0L,
                amplitudes = emptyList(),
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
                        playJob?.cancel()
                        playJob = null
                        _uiState.update {
                            it.copy(phase = RecorderPhase.READY, positionMs = 0L)
                        }
                    }
                    _uiState.update { it.copy(phase = RecorderPhase.PLAYING) }
                    startPlayPolling()
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

    /**
     * 播放中每約 [PLAY_POLL_MS] 同步一次播放位置與總長。
     * 暫停時輪詢仍可繼續(位置不動);停止/播畢/刪除時取消。
     */
    private fun startPlayPolling() {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(
                        positionMs = player.currentPositionMs.toLong(),
                        durationMs = player.durationMs.toLong()
                    )
                }
                delay(PLAY_POLL_MS)
            }
        }
    }

    /** 跳轉到指定播放位置(毫秒)。 */
    fun seekTo(ms: Long) {
        player.seekTo(ms.toInt())
        _uiState.update { it.copy(positionMs = ms) }
    }

    fun stopPlaying() {
        player.stop()
        playJob?.cancel()
        playJob = null
        _uiState.update { it.copy(phase = RecorderPhase.READY, positionMs = 0L) }
    }

    fun deleteCurrent() {
        player.stop()
        recordJob?.cancel()
        recordJob = null
        playJob?.cancel()
        playJob = null
        recordedFile?.delete()
        currentUri = null
        recordedFile = null
        _uiState.update {
            it.copy(
                phase = RecorderPhase.IDLE,
                displayName = "",
                displayPath = "",
                elapsedMs = 0L,
                amplitudes = emptyList(),
                positionMs = 0L,
                durationMs = 0L,
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
        recordJob?.cancel()
        playJob?.cancel()
        recorder.stop()
        player.release()
    }
}
