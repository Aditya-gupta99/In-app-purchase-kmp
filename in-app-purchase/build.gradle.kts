import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.aditya-gupta99"
version = "1.0.7"

kotlin {
    androidTarget {
        publishLibraryVariants("release","debug")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "in-app-purchase"
            isStatic = true
        }
    }



    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.billing.ktx)
                implementation(libs.lifecycle.process)
            }
        }
    }
}

android {
    namespace = "com.aditya.gupta99.inAppPurchase"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {

    coordinates(group.toString(), "inAppPurchase-kmp", version.toString())

    pom {
        name = "InAppPurchase KMP"
        description = "A multiplatform library for In-App Purchases"
        inceptionYear = "2025"
        url = "https://github.com/Aditya-gupta99/In-app-purchase-kmp"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
                distribution = "ZZZ"
            }
        }
        developers {
            developer {
                id = "Aditya-gupta99"
                name = "Aditya Gupta"
                url = "https://github.com/Aditya-gupta99"
            }
        }
        scm {
            url = "https://github.com/Aditya-gupta99/In-app-purchase-kmp"
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}

task("testClasses") {}