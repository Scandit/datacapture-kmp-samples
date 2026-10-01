@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

repositories {
    google()
    mavenCentral()
}

val skipIos = providers.gradleProperty("kmp.skipIos").orNull?.toBoolean() == true

val iosDeviceOnly = providers.gradleProperty("kmp.iosDeviceOnly").orNull?.toBoolean() == true

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
    if (!skipIos) {
        // Bundle the sample's shared module as an XCFramework that the iosApp
        // consumes through the local ScanditKmpPackage SPM definition. The
        // umbrella framework re-exports the core module's surface so SwiftUI
        // sees DataCaptureView / Camera / etc. without separate imports.
        val xcframeworkBundle = XCFramework("shared")
        val iosTargets =
            if (iosDeviceOnly) listOf(iosArm64())
            else listOf(iosArm64(), iosX64(), iosSimulatorArm64())
        iosTargets.forEach { target ->
            target.binaries.framework {
                baseName = "shared"
                isStatic = true
                export("com.scandit.datacapture.kmp:core:8.6.1")
                export("com.scandit.datacapture.kmp:barcode:8.6.1")
                export("com.scandit.datacapture.kmp:label:8.6.1")
                xcframeworkBundle.add(this)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("com.scandit.datacapture.kmp:core:8.6.1")
            api("com.scandit.datacapture.kmp:barcode:8.6.1")
            api("com.scandit.datacapture.kmp:label:8.6.1")
            // Container module shipping the Smart Label Capture text-recognition
            // model (AAR/xcframework, no Kotlin API). Required at runtime — without
            // it LabelCapture crashes when the mode loads its model. Not exported
            // to the iOS XCFramework (no public surface); just linked in.
            api("com.scandit.datacapture.kmp:label-text:8.6.1")
            implementation(libs.kotlinx.coroutines.core)
            // Compose Multiplatform — keeps the scanner screen + alert in commonMain.
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}

android {
    namespace = "com.scandit.kmp.labelcapturesimplesample.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
