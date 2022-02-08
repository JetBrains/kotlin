plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    android()

    val macosX64 = macosX64()
    val iosX64 = iosX64()
    val iosSimulatorArm64 = iosSimulatorArm64()
    val iosArm64 = iosArm64()
    configure(listOf(macosX64, iosX64, iosSimulatorArm64, iosArm64))  {
        binaries {
            framework {
                baseName = "sdk"
            }
            framework("custom") {
                baseName = "lib"
            }
        }
    }
    sourceSets {
        val commonMain by getting

        val iosX64Main by getting
        val iosSimulatorArm64Main by getting
        val iosArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}
