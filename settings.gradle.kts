pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral {}
        maven("https://dl.google.com/dl/android/maven2/")
    }
}

rootProject.name = "in-app-purchase-kmp"
include(":in-app-purchase")