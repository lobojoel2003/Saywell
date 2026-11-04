import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import saywell.gradle.PrepareBundledVoice
import saywell.gradle.PrepareOpenMojiSymbols
import saywell.gradle.VerifySha256

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------------------------
// Version and build number
//
// version.properties holds the release version; the build number (versionCode) is derived from
// it so releases always increase: MAJOR * 10000 + MINOR * 100 + PATCH.
// CI passes SAYWELL_BUILD_NUMBER (the workflow run number) and GITHUB_SHA, shown on the About screen.
// ---------------------------------------------------------------------------------------------
val versionProperties = Properties().apply { rootProject.file("version.properties").inputStream().use(::load) }
val appVersion: String = versionProperties.getProperty("version")
    ?: error("version.properties must define version=MAJOR.MINOR.PATCH")
val versionParts = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(appVersion)?.groupValues?.drop(1)?.map(String::toInt)
    ?: error("version.properties: '$appVersion' is not MAJOR.MINOR.PATCH")
require(versionParts[1] < 100 && versionParts[2] < 100) { "MINOR and PATCH must be below 100" }
val appVersionCode = versionParts[0] * 10000 + versionParts[1] * 100 + versionParts[2]
val buildNumber = providers.environmentVariable("SAYWELL_BUILD_NUMBER").orElse("0").get()
val gitCommit = providers.environmentVariable("GITHUB_SHA").map { it.take(12) }.orElse("local").get()

// ---------------------------------------------------------------------------------------------
// Release signing, from environment variables so keys never live in the repository.
// See docs/releasing.md.
// ---------------------------------------------------------------------------------------------
val releaseKeystore = providers.environmentVariable("SAYWELL_KEYSTORE_FILE").orNull

android {
    namespace = "io.github.lobojoel2003.saywell"
    // Compose 1.12 and Coil 3.6 need the Android 17 SDK to compile against; the app still targets Android 16.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.lobojoel2003.saywell"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("String", "BUILD_NUMBER", "\"$buildNumber\"")
        buildConfigField("String", "BUNDLED_VOICE", "\"${libs.versions.bundledVoice.get().removePrefix("vits-piper-")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("SAYWELL_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("SAYWELL_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("SAYWELL_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // One APK per processor type keeps downloads small; the universal APK runs everywhere.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // Stored uncompressed so the voice model loads straight from the APK.
        noCompress += "onnx"
    }

    packaging {
        jniLibs {
            // 32-bit x86 devices are rare; leave them out of the universal APK.
            excludes += "lib/x86/**"
        }
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/NOTICE*", "/META-INF/*.kotlin_module")
        }
    }

    // F-Droid and reproducible builds: don't embed Google's encrypted dependency report.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = true
        // Print every issue in the build log, not only the first.
        textReport = true
        textOutput = File("stdout")
        checkReleaseBuilds = true
        // New library versions are handled by Dependabot, not by failing lint.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
        // Sherpa-ONNX is declared with an explicit @aar extension (it comes from an ivy repository),
        // which version-catalog entries can't express.
        disable += "UseTomlInstead"
        // android.media.ExifInterface reads streams correctly from API 24, Saywell's minimum, so the
        // androidx backport isn't needed.
        disable += "ExifInterface"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "3g"
                // ScreenshotTest renders the main screens to app/build/screenshots when set to true.
                it.systemProperty("saywell.screenshots", providers.gradleProperty("saywell.screenshots").getOrElse("false"))
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

// ---------------------------------------------------------------------------------------------
// Assets made at build time from pinned, checksummed downloads
// ---------------------------------------------------------------------------------------------
val openMojiArchive: Configuration = configurations.create("openMojiArchive")
val sherpaOnnxAar: Configuration = configurations.create("sherpaOnnxAar")
val bundledVoiceArchive: Configuration = configurations.create("bundledVoiceArchive")
val bundleVoice = providers.gradleProperty("saywell.bundleVoice").map(String::toBoolean).orElse(true).get()

dependencies {
    add(openMojiArchive.name, "npm:openmoji:${libs.versions.openmoji.get()}@tgz")
    add(sherpaOnnxAar.name, "com.k2fsa:sherpa-onnx:${libs.versions.sherpaOnnx.get()}@aar")
    if (bundleVoice) add(bundledVoiceArchive.name, "com.k2fsa.voices:${libs.versions.bundledVoice.get()}:tts-models@tar.bz2")
}

val prepareOpenMoji = tasks.register<PrepareOpenMojiSymbols>("prepareOpenMoji") {
    description = "Unpacks OpenMoji symbols and their search index into the app's assets."
    archive.from(openMojiArchive)
    expectedSha256 = libs.versions.openmojiSha256
    outputDirectory = layout.buildDirectory.dir("generated/saywell/openmoji")
}

val prepareBundledVoice = tasks.register<PrepareBundledVoice>("prepareBundledVoice") {
    description = "Unpacks the bundled Piper voice and espeak-ng data into the app's assets."
    archive.from(bundledVoiceArchive)
    expectedSha256 = libs.versions.bundledVoiceSha256
    voicePackage = libs.versions.bundledVoice
    keepDictionaries = listOf("en_dict")
    outputDirectory = layout.buildDirectory.dir("generated/saywell/voice")
}

val verifySherpaOnnx = tasks.register<VerifySha256>("verifySherpaOnnx") {
    description = "Checks the Sherpa-ONNX library against its pinned checksum."
    artifact.from(sherpaOnnxAar)
    expectedSha256 = libs.versions.sherpaOnnxSha256
    label = "Sherpa-ONNX ${libs.versions.sherpaOnnx.get()} AAR"
    receipt = layout.buildDirectory.file("generated/saywell/sherpa-onnx.sha256")
}

tasks.named("preBuild") { dependsOn(verifySherpaOnnx) }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(prepareOpenMoji, PrepareOpenMojiSymbols::outputDirectory)
        if (bundleVoice) {
            variant.sources.assets?.addGeneratedSourceDirectory(prepareBundledVoice, PrepareBundledVoice::outputDirectory)
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.k2fsa:sherpa-onnx:${libs.versions.sherpaOnnx.get()}@aar")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.compress)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
