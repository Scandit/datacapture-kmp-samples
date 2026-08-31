enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
        repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LabelCaptureSimpleSample"

include(":androidApp")
include(":shared")
