# 錄音 App 現代化升級計畫

> 目標:把這個 2016 年的 Android 錄音 App,升級為 2026 最新技術棧
> (Kotlin + Jetpack Compose + 完整現代化),並讓它能在本機重新建置。
>
> 決議方向:**Kotlin / Jetpack Compose / 完整現代化 / 錄音檔存放於 App 私有目錄(免儲存權限)**

---

## 一、現況診斷

| 項目 | 現況 | 問題 |
|---|---|---|
| 建置工具 | Gradle 5.6.4 + AGP 3.6.2 | **本機 JDK 26 完全跑不起來**(此工具鏈上限約 JDK 12) |
| 語言/UI | Java + XML View + `findViewById` | 非官方主流,`android.support` 已淘汰 |
| Target SDK | 24 (2016) | 落後本機可用的 SDK 36 共 12 個版本 |
| 儲存 | 寫入 `Environment.getExternalStorageDirectory()/Recoeds/` | **Android 10+ Scoped Storage 下此寫法直接失效** |
| 權限 | `WRITE/READ_EXTERNAL_STORAGE` | Android 10+ 形同無效,且非必要 |
| 錄音 API | `MediaRecorder()` 無參數建構 + AMR_NB/3gp | API 31+ 已棄用建構式,格式老舊 |
| 選檔 | `content://` 強轉 `file://`(`convertUri`) | 現代 Android 會直接崩潰 |
| 設定檔 | `local.properties` 指向 Windows 路徑、用 `jcenter()`/`compile` | 失效來源 + 已關閉的倉庫 |

**結論**:這不是「要不要升級」,而是「不升級就無法建置」。

---

## 二、目標技術棧

| 層面 | 目標 |
|---|---|
| 建置 | Gradle 8.x(最新穩定)+ AGP 8.x、Kotlin DSL(`.kts`)+ Version Catalog(`libs.versions.toml`) |
| **建置 JDK** | **JDK 17 或 21 LTS**(不要用 JDK 26;AGP 尚未正式支援,風險高) |
| 語言 | Kotlin 2.x |
| UI | Jetpack Compose(Compose BOM 最新版)+ Material 3 |
| SDK | `compileSdk 36` / `targetSdk 36` / `minSdk 24`(合理涵蓋率,可調) |
| 架構 | `ViewModel` + `StateFlow` 管理錄音/播放狀態(務實,不過度導入 DI) |
| 倉庫 | `google()` + `mavenCentral()`(移除死掉的 jcenter) |

---

## 三、核心行為對照(舊 → 新)

| 功能 | 舊寫法 | 新寫法 |
|---|---|---|
| 儲存位置 | 外部根目錄 `Recoeds/`(需權限、會失效) | `context.getExternalFilesDir(DIRECTORY_MUSIC)` —— **App 私有目錄,免任何儲存權限** |
| 錄音格式 | 3gp / AMR_NB | **m4a / AAC**(`MPEG_4` + `AAC`,相容性與音質佳) |
| Recorder 建構 | `new MediaRecorder()` | `MediaRecorder(context)`(API 31+ 規範) |
| 權限請求 | 自寫 `ActivityCompat.requestPermissions` | Compose `rememberLauncherForActivityResult` + **只剩 `RECORD_AUDIO`** |
| 選檔 | `ACTION_GET_CONTENT` + content→file 強轉 | `ActivityResultContracts.OpenDocument`,直接以 `Uri` 餵給 `MediaPlayer.setDataSource(context, uri)` |
| UI 狀態 | 各按鈕手動 `setEnabled` | 由 `StateFlow` 單一狀態源驅動 Composable,杜絕非法狀態 |
| 螢幕常亮 | `WindowManager` flag | Compose `KeepScreenOn` 效果 |

---

## 四、儲存策略(已決議)

採用 **App 私有外部目錄**:`context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)`。

- 免任何儲存權限,跨所有 Android 版本皆可寫入。
- App 解除安裝時檔案會一併移除(對單機錄音工具是合理行為)。
- 因此 `AndroidManifest.xml` 只需保留 `RECORD_AUDIO` 權限,其餘儲存相關權限全部移除。
- 選檔播放仍透過 SAF(`OpenDocument`)以 `Uri` 直接播放,不複製檔案。

---

## 五、分階段執行計畫

### Phase 0 — 環境修復(讓它能 build)
1. 修正 `local.properties`(改為 macOS SDK 路徑,並確認已在 `.gitignore`)。
2. 確認/安裝 JDK 17 或 21,設定 Gradle 使用該 JDK(`org.gradle.java.home` 或 `JAVA_HOME`)。
3. 升級 `gradle-wrapper.properties` 到 Gradle 8.x。

### Phase 1 — 建置系統現代化
4. 根 `build.gradle` → `build.gradle.kts`,改用 plugins DSL,移除 `jcenter()`。
5. 建立 `gradle/libs.versions.toml` 集中管理版本。
6. `app/build.gradle` → `.kts`:加入 `namespace`、Kotlin、Compose、Compose Compiler plugin;`compile`→`implementation`;移除 support library。
7. `AndroidManifest.xml` 移除 `package` 屬性(改由 `namespace`)、刪除多餘儲存權限,只留 `RECORD_AUDIO`。

### Phase 2 — Kotlin + Compose 重寫
8. 新增 `AudioRecorder` / `AudioPlayer` 小型封裝類(包住 `MediaRecorder`/`MediaPlayer` 生命週期)。
9. `RecorderViewModel`:以 `StateFlow<RecorderUiState>` 表達「閒置/錄音中/可播放/播放中」等狀態。
10. `RecorderScreen` Composable:重現原本所有按鈕(錄音/停止/播放/停止播放/選檔/刪除/權限)與檔名、路徑顯示,UI 啟用狀態全部由 state 推導。
11. `MainActivity` 改為 `ComponentActivity` + `setContent { }`,刪除舊 Java。

### Phase 3 — 驗證與收尾
12. `./gradlew assembleDebug` 確認編譯通過。
13. 實機/模擬器走一輪:授權 → 錄音 → 播放/暫停 → 停止 → 選檔播放 → 刪除。
14. 更新 `CLAUDE.md` 反映新技術棧;清理 `projectFilesBackup/`、`.idea/` 等過時檔案。

---

## 六、風險與注意事項

- **JDK 版本**是最大卡點:必須用 JDK 17/21 建置,JDK 26 需以 `org.gradle.java.home` 或 `JAVA_HOME` 明確指定。
- 既有檔案目錄名 `Recoeds`(原始碼拼字)在私有目錄重建後不影響相容,將改成正確命名。
- 這是**重寫等級**的改動,但會保留在現有 git repo、分支上進行,功能對使用者維持一致。
- 版本號(Gradle / AGP / Kotlin / Compose BOM)以動工當下的最新穩定版為準,計畫執行時會再次確認。
