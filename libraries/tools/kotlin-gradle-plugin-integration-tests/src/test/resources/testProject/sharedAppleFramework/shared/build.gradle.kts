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
        val commonMain = getByName("commonMain")

        val iosX64Main = getByName("iosX64Main")
        val iosSimulatorArm64Main = getByName("iosSimulatorArm64Main")
        val iosArm64Main = getByName("iosArm64Main")

        val iosMain = create("iosMain") {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
        }
    }
}
