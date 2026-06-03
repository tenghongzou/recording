package com.example.zou.recording

/**
 * 錄音流程的狀態階段,直接驅動 UI 上各按鈕的啟用/停用,
 * 取代舊版散落各處的 setEnabled(...) 手動管理。
 */
enum class RecorderPhase {
    /** 閒置:可錄音、可輸入檔名 */
    IDLE,

    /** 錄音中:只可停止錄音 */
    RECORDING,

    /** 已有錄音或已選取檔案:可播放、刪除、重新錄音、選檔 */
    READY,

    /** 播放中:可停止播放、可暫停 */
    PLAYING,

    /** 已暫停:可繼續播放、可停止播放 */
    PAUSED
}

/**
 * 單一狀態來源(single source of truth),由 [RecorderViewModel] 以 StateFlow 發布、
 * 由 RecorderScreen 觀察。所有 UI 呈現與按鈕啟用狀態皆從此推導。
 */
data class RecorderUiState(
    val phase: RecorderPhase = RecorderPhase.IDLE,
    /** EditText 的檔名輸入(不含副檔名) */
    val fileName: String = "",
    /** 顯示用:目前檔案名稱 */
    val displayName: String = "",
    /** 顯示用:目前檔案的完整路徑或來源 */
    val displayPath: String = "",
    /** 是否已取得錄音(RECORD_AUDIO)權限 */
    val hasPermission: Boolean = false,
    /** 一次性提示訊息(對應舊版的 Toast),顯示後由 UI 呼叫 consumeMessage 清除 */
    val message: String? = null
)
