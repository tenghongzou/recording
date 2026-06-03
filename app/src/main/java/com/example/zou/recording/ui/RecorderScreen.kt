package com.example.zou.recording.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zou.recording.RecorderPhase
import com.example.zou.recording.RecorderUiState
import com.example.zou.recording.RecorderViewModel
import com.example.zou.recording.ui.theme.RecordingTheme

/** 將毫秒轉成 mm:ss。 */
fun formatMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0)) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * 即時波形:把 amplitudes(0f..1f)畫成一排垂直長條,由左到右、置中對齊。
 * 空清單時畫一條置中基準線。
 */
@Composable
fun Waveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        if (amplitudes.isEmpty()) {
            drawLine(
                color = barColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val count = amplitudes.size
        val slot = size.width / count
        val barWidth = (slot * 0.6f).coerceAtLeast(2f)
        amplitudes.forEachIndexed { index, raw ->
            val amp = raw.coerceIn(0f, 1f)
            val barHeight = (amp * size.height).coerceAtLeast(barWidth)
            val cx = slot * index + slot / 2f
            drawLine(
                color = barColor,
                start = Offset(cx, centerY - barHeight / 2f),
                end = Offset(cx, centerY + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 無狀態版面:吃 [RecorderUiState] 與一組 lambda,方便 Preview 與測試。
 * [RecorderScreen] 負責接 viewModel / launcher 後呼叫它。
 */
@Composable
fun RecorderContent(
    state: RecorderUiState,
    snackbarHostState: SnackbarHostState,
    onFileNameChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDelete: () -> Unit,
    onPickFile: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val phase = state.phase
    val isRecording = phase == RecorderPhase.RECORDING
    val isPlaybackVisible = phase == RecorderPhase.READY ||
        phase == RecorderPhase.PLAYING ||
        phase == RecorderPhase.PAUSED

    // 頂部大字計時:錄音中 = elapsedMs;播放/暫停 = positionMs;其餘 00:00。
    val timerMs = when (phase) {
        RecorderPhase.RECORDING -> state.elapsedMs
        RecorderPhase.PLAYING, RecorderPhase.PAUSED -> state.positionMs
        else -> 0L
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "錄音機",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            // 計時 + 波形卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = formatMs(timerMs),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Waveform(
                        amplitudes = state.amplitudes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                    )
                }
            }

            // 主錄音鍵:大型圓形按鈕
            RecordButton(
                isRecording = isRecording,
                onClick = { if (isRecording) onStopRecording() else onStartRecording() }
            )

            // 播放區(僅 READY/PLAYING/PAUSED 顯示)
            AnimatedVisibility(visible = isPlaybackVisible) {
                PlaybackControls(
                    isPlaying = phase == RecorderPhase.PLAYING,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo
                )
            }

            // 檔名輸入
            OutlinedTextField(
                value = state.fileName,
                onValueChange = onFileNameChange,
                label = { Text("錄音名稱") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )

            // 次要操作列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onPickFile,
                    enabled = phase != RecorderPhase.RECORDING,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("選擇")
                }
                FilledTonalButton(
                    onClick = onDelete,
                    enabled = phase == RecorderPhase.READY,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("刪除")
                }
                FilledTonalButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("權限")
                }
            }

            // 顯示資訊
            if (state.displayName.isNotBlank() || state.displayPath.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (state.displayName.isNotBlank()) {
                        Text(
                            text = "名稱:${state.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.displayPath.isNotBlank()) {
                        Text(
                            text = "路徑:${state.displayPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** 大型圓形主錄音鍵,錄音中有呼吸縮放動畫。 */
@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "record-breath")
    val breathScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val scale = if (isRecording) breathScale else 1f

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isRecording) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        modifier = Modifier
            .size(112.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRecording) "停止錄音" else "開始錄音",
                modifier = Modifier.size(48.dp),
                tint = if (isRecording) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

/** 播放區:播放/暫停鍵 + 進度條 + 兩側時間。 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    // 拖動時暫存值,放開時 seekTo。
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val maxValue = durationMs.coerceAtLeast(1).toFloat()
    val sliderValue = (dragValue ?: positionMs.toFloat()).coerceIn(0f, maxValue)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePlayPause,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暫停" else "播放",
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = formatMs(positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Slider(
                value = sliderValue,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { onSeekTo(it.toLong()) }
                    dragValue = null
                },
                valueRange = 0f..maxValue,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = formatMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun RecorderScreen(viewModel: RecorderViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFilePicked(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    RecorderContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onFileNameChange = viewModel::onFileNameChange,
        onStartRecording = { viewModel.startRecording() },
        onStopRecording = { viewModel.stopRecording() },
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSeekTo = { viewModel.seekTo(it) },
        onDelete = { viewModel.deleteCurrent() },
        onPickFile = { pickLauncher.launch(arrayOf("audio/*")) },
        onRequestPermission = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    )
}

@Preview(showBackground = true)
@Composable
private fun RecorderContentIdlePreview() {
    RecordingTheme {
        RecorderContent(
            state = RecorderUiState(
                phase = RecorderPhase.IDLE,
                fileName = "我的錄音",
                displayName = "—",
                displayPath = "—"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFileNameChange = {},
            onStartRecording = {},
            onStopRecording = {},
            onTogglePlayPause = {},
            onSeekTo = {},
            onDelete = {},
            onPickFile = {},
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecorderContentRecordingPreview() {
    RecordingTheme {
        RecorderContent(
            state = RecorderUiState(
                phase = RecorderPhase.RECORDING,
                fileName = "我的錄音",
                elapsedMs = 73_000,
                amplitudes = List(40) { (kotlin.math.sin(it * 0.4) * 0.4 + 0.5).toFloat() }
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFileNameChange = {},
            onStartRecording = {},
            onStopRecording = {},
            onTogglePlayPause = {},
            onSeekTo = {},
            onDelete = {},
            onPickFile = {},
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecorderContentReadyPreview() {
    RecordingTheme {
        RecorderContent(
            state = RecorderUiState(
                phase = RecorderPhase.PLAYING,
                fileName = "我的錄音",
                displayName = "我的錄音.m4a",
                displayPath = "/storage/.../我的錄音.m4a",
                positionMs = 25_000,
                durationMs = 90_000
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFileNameChange = {},
            onStartRecording = {},
            onStopRecording = {},
            onTogglePlayPause = {},
            onSeekTo = {},
            onDelete = {},
            onPickFile = {},
            onRequestPermission = {}
        )
    }
}
