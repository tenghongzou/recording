// 專案層級 build 設定;各 plugin 在此宣告(apply false),由子模組實際套用。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
