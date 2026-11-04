plugins {
    alias(libs.plugins.android.application) apply false
    // Declared here so the whole build (including AGP's built-in Kotlin support) uses one Kotlin version.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
