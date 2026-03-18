plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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
            framework("withoutSymbolicLink") {
                baseName = "withoutSymbolicLink"
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
