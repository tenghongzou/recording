# 簡易錄音機 (Recording)

一個 Android 麥克風錄音 App:錄音、播放、刪除、選取音檔。

最初為 2016 年的 Java / XML 練習專案(第一次實作 MIC 功能),已完整現代化為 **Kotlin + Jetpack Compose** 技術棧。

## 功能

- 🎙 以麥克風錄音,輸出 **m4a / AAC**
- ▶️ 播放 / 暫停 / 繼續 / 停止
- 🗑 刪除錄音
- 📂 透過系統選檔器(SAF）挑選音檔播放
- 🔐 執行階段請求錄音權限

## 技術棧

| 項目 | 版本 / 技術 |
|---|---|
| 語言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| 架構 | `ViewModel` + `StateFlow` 單向資料流;audio / viewmodel / ui 分層 |
| 建置 | Gradle 8.13 + AGP 8.11、Kotlin DSL + Version Catalog |
| SDK | compileSdk / targetSdk 36、minSdk 24 |
| 儲存 | App 私有目錄 `getExternalFilesDir`(免儲存權限) |

## 開發

需以 **JDK 21** 建置(已於 `gradle.properties` 指定;`org.gradle.java.home` 為本機特定路徑,換機需調整)。

```bash
./gradlew assembleDebug   # 編譯 debug APK
./gradlew installDebug    # 安裝到已連線的裝置 / 模擬器
./gradlew test            # JVM 單元測試
```

更多架構與設計說明見 [`CLAUDE.md`](CLAUDE.md);完整現代化歷程見 [`UPGRADE_PLAN.md`](UPGRADE_PLAN.md) 與 git log 的 Phase 0–3 提交。
