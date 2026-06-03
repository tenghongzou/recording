# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概觀

這是一個 Android 錄音 App(`簡易錄音程式`):麥克風（MIC）錄音、播放、刪除與選檔。原為 2016 年的 Java/XML 專案,已現代化為 **Kotlin + Jetpack Compose**(見 `UPGRADE_PLAN.md` 的升級歷程,git log 中 Phase 0–3 的提交記錄了完整遷移)。

## 工具鏈版本(重要)

動建置設定前務必注意這套版本相依:

- **建置 JDK 必須是 JDK 21**(Temurin 21)。本機另有 JDK 26,但 AGP 不支援;`gradle.properties` 以 `org.gradle.java.home` 指定 JDK 21。換機器時需同步調整此路徑(屬本機特定設定)。
- Gradle **8.13**(wrapper)、AGP **8.11.1**、Kotlin **2.0.21**。
- `compileSdk`/`targetSdk` **36**、`minSdk` **24**、Java/Kotlin target **17**。
- 採 **Gradle Kotlin DSL**(`*.gradle.kts`)+ **Version Catalog**。所有版本集中在 `gradle/libs.versions.toml`,**不要**在 build 腳本內寫死版本號,改用 `libs.*` alias。
- 倉庫為 `google()` + `mavenCentral()`(已移除停用的 jcenter)。

## 常用指令

使用 Gradle wrapper(本機已透過 `org.gradle.java.home` 綁定 JDK 21):

```bash
./gradlew assembleDebug        # 編譯 debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # 編譯並安裝到已連線的裝置/模擬器
./gradlew test                 # JVM 單元測試（app/src/test/）
./gradlew connectedAndroidTest # 需裝置的 instrumented 測試（app/src/androidTest/）
./gradlew lint                 # Android Lint
./gradlew clean
```

執行單一單元測試:

```bash
./gradlew test --tests "com.example.zou.recording.ExampleUnitTest.addition_isCorrect"
```

模擬器驗證(本機有 AVD `Medium_Phone_API_36.0`):

```bash
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.0 &   # 啟動
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.zou.recording/.MainActivity
```

## 程式架構

單一畫面 App,以 **單向資料流 + 狀態機** 取代舊版散落的 `setEnabled` 手動控制。全部 Kotlin 位於 `app/src/main/java/com/example/zou/recording/`:

- **`RecorderUiState.kt`** — 狀態契約(單一來源)。`RecorderPhase` 列舉(`IDLE / RECORDING / READY / PLAYING / PAUSED`)是整個 App 的核心狀態機;`RecorderUiState` data class 是唯一的 UI 狀態。**所有按鈕的啟用/停用、播放鍵文字都從 `phase` 推導**,新增流程時改這裡的狀態機而非各別 widget。
- **`RecorderViewModel.kt`** — `AndroidViewModel`,以 `MutableStateFlow<RecorderUiState>` 對外曝露唯讀 `StateFlow`。集中所有流程控制與 phase 轉換,持有 `AudioRecorder`/`AudioPlayer`,並以私有的 `currentUri`/`recordedFile` 追蹤目前音檔。`message` 欄位是一次性提示(對應舊 Toast),UI 顯示後呼叫 `consumeMessage()` 清除。
- **`audio/AudioRecorder.kt`、`audio/AudioPlayer.kt`** — 薄封裝,隔離 `MediaRecorder`/`MediaPlayer` 的生命週期與例外。ViewModel 只透過它們的公開 API 操作,不直接碰 Android media 類別。
- **`ui/RecorderScreen.kt`** — Compose 畫面。以 `collectAsState()` 觀察 ViewModel;權限與選檔走 `rememberLauncherForActivityResult`(`RequestPermission` / `OpenDocument`);`message` 透過 `Scaffold` + `SnackbarHost` 呈現。
- **`ui/theme/`** — Material3 主題(`RecordingTheme`)。
- **`MainActivity.kt`** — `ComponentActivity` + `setContent { RecordingTheme { RecorderScreen() } }`,僅作為宿主。

資料流向:`RecorderScreen`(事件)→ `RecorderViewModel`(更新 `StateFlow`)→ `RecorderScreen`(重組)。新增功能時依此鏈路:先在 state machine 表達狀態,再於 ViewModel 實作轉換,最後 UI 綁定。

## 儲存與權限(關鍵設計決策)

- 錄音檔寫入 **App 私有外部目錄** `context.getExternalFilesDir(DIRECTORY_MUSIC)`,格式 **m4a / AAC**。此目錄**免任何儲存權限**,跨 Android 版本皆可寫;App 解除安裝時檔案隨之移除。
- 因此 `AndroidManifest.xml` **只宣告 `RECORD_AUDIO`** 一個權限。不要為了存檔加回 `WRITE_EXTERNAL_STORAGE`——那在現代 Android 無效且非必要。
- 選檔播放透過 SAF 以 `Uri` 直接餵給 `MediaPlayer`,不做舊版的 `content://`→`file://` 轉換。

## 注意事項

- 新增依賴/外掛:先在 `gradle/libs.versions.toml` 定義 alias,再於 build 腳本 `libs.` 引用。
- Compose 編譯由 Kotlin Compose Compiler plugin(`org.jetbrains.kotlin.plugin.compose`,版本綁 Kotlin)處理,已在 catalog 與 app 模組設定。
- `gradle-wrapper.jar` 是版控內必要檔(`.gitignore` 對 `*.jar` 有 `!gradle/wrapper/gradle-wrapper.jar` 例外),勿刪。
