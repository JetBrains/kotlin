plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosSimulatorArm64 {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    optIn.addAll(
                        "kotlinx.cinterop.ExperimentalForeignApi",
                        "kotlinx.cinterop.BetaInteropApi"
                    )
                }
            }
        }

        binaries {
            testBundle("iosAppTests") {
                linkerOpts = mutableListOf(
                    "-F",
                    "<PATH_TO_FRAMEWORKS>"
                )
                binaryOption("bundleId", "iosAppTests")
            }
        }
    }

    sourceSets {
        nativeTest.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-test-native-xctest:2.1.255-SNAPSHOT")
        }
    }
}