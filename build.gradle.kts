// Top-level build file. AGP 9 provides built-in Kotlin, so org.jetbrains.kotlin.android is NOT applied.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
