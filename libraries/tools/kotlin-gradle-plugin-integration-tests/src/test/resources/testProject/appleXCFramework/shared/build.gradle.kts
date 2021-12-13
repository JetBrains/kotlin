import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
}

kotlin {
    val xcf = XCFramework()
    val otherXCFramework = XCFramework("other")

    ios {
        binaries {
            framework {
                baseName = "shared"
                xcf.add(this)
                otherXCFramework.add(this)
            }
        }
    }

    watchos {
        binaries {
            framework {
                baseName = "shared"
                xcf.add(this)
            }
        }
    }
}
