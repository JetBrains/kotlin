import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        binaries {
            sharedLib(namePrefix = "shared")
            staticLib(namePrefix = "static")
            executable(namePrefix = "executable") { entryPoint = "main" }
        }
    }
    iosX64 {
        binaries {
            framework {
                baseName = "libFramework"
            }
        }
    }
    iosArm64 {
        binaries {
            framework {
                baseName = "libFramework"
            }
        }
    }
    iosSimulatorArm64()

    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.compilations {
            val main by getting {
                cinterops {
                    val myCinterop by creating {
                        defFile(project.file("src/my.def"))
                    }
                }
            }
        }
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
