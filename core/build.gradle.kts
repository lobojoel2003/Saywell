import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Platform-independent Saywell logic: boards, Open Board Format import/export, the message
// bar, switch scanning, symbol search and word prediction. No Android dependencies, so it is
// fast to test on any JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
