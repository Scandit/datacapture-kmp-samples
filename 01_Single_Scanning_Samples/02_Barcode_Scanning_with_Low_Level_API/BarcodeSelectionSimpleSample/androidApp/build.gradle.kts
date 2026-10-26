plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.scandit.kmp.barcodeselectionsimplesample"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.scandit.kmp.barcodeselectionsimplesample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = (rootProject.ext["frameworks_sdk_version"] as String?) ?: "1.0.0"
        // Ship 64-bit ABIs only: arm64-v8a covers physical devices + Apple
        // Silicon Mac emulators; x86_64 covers Intel/Linux emulators. The
        // obsolete 32-bit variants (armeabi-v7a, x86) carried a copy of
        // ScanditCaptureCore.so each (~10 MB) for diminishing returns.
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // R8 + resource shrinking on release. Compose, AndroidX,
            // kotlinx-coroutines, and the Scandit SDK AARs ship
            // consumer-proguard-rules.txt so R8 picks up the keeps for
            // reflection/JNI-accessible classes automatically; the local
            // proguard-rules.pro is empty so consumers have an obvious
            // place to add app-specific keeps.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Compose tooling-preview is a debug-only need (Android Studio preview);
    // shipping it in release adds dead weight.
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)

    // Required to reach the core view bridging types (AndroidViewContext,
    // toAndroidView) directly from the Activity, since `:shared` exposes them
    // transitively but compose host code references them by FQN.
    implementation("com.scandit.datacapture.kmp:core:8.6.2")
}
