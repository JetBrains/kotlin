import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
}

kotlin {
    val sdkXCFramework = XCFramework("sdk")
    val otherXCFramework = XCFramework()

    ios {
        binaries {
            framework {
                baseName = "shared"
                sdkXCFramework.add(this)
                otherXCFramework.add(this)
            }
        }
    }

    watchos {
        binaries {
            framework {
                baseName = "shared"
                sdkXCFramework.add(this)
            }
        }
    }
}
