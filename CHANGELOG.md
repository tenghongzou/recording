# Change Log

本專案所有重大變更記錄於此。日期格式為 YYYY-MM-DD。

## [2.0.0] - 2026-06-03 — 全面現代化(Kotlin + Jetpack Compose)

將 2016 年的 Java / XML 專案(原已無法在現今工具鏈建置)完整升級為 2026 技術棧,
分四階段進行,並於 Android 16 模擬器實機驗證全功能可運作。

### Added(新增)
- Kotlin + Jetpack Compose + Material 3 介面
- `ViewModel` + `StateFlow` 單向資料流與狀態機(`RecorderPhase`)
- 分層架構:`audio/`(AudioRecorder、AudioPlayer)、ViewModel、`ui/`(Screen、theme)
- Gradle Version Catalog(`gradle/libs.versions.toml`)集中版本管理
- `CLAUDE.md`、`UPGRADE_PLAN.md` 專案文件

### Changed(變更)
- 語言由 Java 改為 Kotlin 2.0.21
- UI 由 XML View + `findViewById` 改為 Compose 宣告式 UI
- 建置由 Groovy + AGP 3.6.2 改為 Kotlin DSL + AGP 8.11.1 + Gradle 8.13
- 建置 JDK 改用 JDK 21(原工具鏈無法在現今 JDK 執行)
- `compileSdk` / `targetSdk` 由 24 提升至 36,`minSdk` 15 → 24
- 錄音格式由 3gp / AMR_NB 改為 m4a / AAC,並改用 `MediaRecorder(context)`
- 儲存改用 App 私有目錄 `getExternalFilesDir`(免儲存權限)
- 權限與選檔改用 ActivityResult API(`RequestPermission` / `OpenDocument`)
- 移除已停用的 jcenter,改用 google() + mavenCentral()
- 啟用 AndroidX(`android.useAndroidX` + `nonTransitiveRClass`)
- README 改寫,反映新功能與技術棧

### Removed(移除)
- `WRITE/READ_EXTERNAL_STORAGE`、`MOUNT_UNMOUNT_FILESYSTEMS` 權限(僅保留 `RECORD_AUDIO`)
- 舊版 `content://` → `file://` 路徑強制轉換
- 過時檔案:`MainActivity.java`、`activity_main.xml`、`styles.xml`(Theme.AppCompat)、
  失效的 `ApplicationTest.java`、無用的 `colors.xml` / `dimens.xml`

### Fixed(修復)
- 修正 `local.properties` 由 Windows 路徑改為本機 macOS SDK 路徑,並取消 git 追蹤
- 補回從未進版控的 `gradle-wrapper.jar`
- repo 衛生:取消追蹤 `.idea/`、`*.iml`、`build/`、刪除 `projectFilesBackup/`

### 對應提交
- Phase 0(環境修復):`c880a06`
- Phase 1(建置現代化):`1d3e52c`
- Phase 2(Kotlin/Compose 重寫):`22cb951`
- Phase 3(驗證、清理、文件):`fc431f6`
- README 更新:`621825f`

## [1.0] - 2016 — 初版

- 以 Java + XML 實作的基本 MIC 錄音功能(錄音、播放、刪除、選檔)。
