package com.example.zou.recording

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.zou.recording.ui.RecorderScreen
import com.example.zou.recording.ui.theme.RecordingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沿用舊版:錄音時螢幕不休眠
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            RecordingTheme {
                Surface {
                    RecorderScreen()
                }
            }
        }
    }
}
