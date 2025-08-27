// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // 在这里添加 Hilt 插件的声明
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}