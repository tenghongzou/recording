package com.example.zou.recording.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zou.recording.RecorderPhase
import com.example.zou.recording.RecorderViewModel
import com.example.zou.recording.ui.theme.RecordingTheme

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

    val phase = state.phase

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "簡易錄音機",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = state.fileName,
                onValueChange = viewModel::onFileNameChange,
                label = { Text("要儲存的檔名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.startRecording() },
                    enabled = phase == RecorderPhase.IDLE || phase == RecorderPhase.READY,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("開始錄音")
                }
                Button(
                    onClick = { viewModel.stopRecording() },
                    enabled = phase == RecorderPhase.RECORDING,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("停止錄音")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.togglePlayPause() },
                    enabled = phase == RecorderPhase.READY ||
                        phase == RecorderPhase.PLAYING ||
                        phase == RecorderPhase.PAUSED,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when (phase) {
                            RecorderPhase.PLAYING -> "暫停播放"
                            RecorderPhase.PAUSED -> "繼續播放"
                            else -> "播放錄音"
                        }
                    )
                }
                Button(
                    onClick = { viewModel.stopPlaying() },
                    enabled = phase == RecorderPhase.PLAYING || phase == RecorderPhase.PAUSED,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("停止播放")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { pickLauncher.launch(arrayOf("audio/*")) },
                    enabled = phase != RecorderPhase.RECORDING,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("選擇檔案")
                }
                Button(
                    onClick = { viewModel.deleteCurrent() },
                    enabled = phase == RecorderPhase.READY,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("刪除檔案")
                }
            }

            Button(
                onClick = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("權限")
            }

            Text("儲存名稱:${state.displayName}")
            Text("檔案路徑:${state.displayPath}")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecorderScreenPreview() {
    RecordingTheme {
        RecorderScreen()
    }
}
