import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
}

val MODULE_NAME = "kotlin-power-assert-runtime"
fun KotlinCommonCompilerOptions.addReturnValueCheckerInfo() {
    freeCompilerArgs.add("-Xreturn-value-checker=full")
}

kotlin {
    explicitApi()

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    optIn.add("kotlin.contracts.ExperimentalContracts")
                }
            }
        }
    }

    metadata { // For common sources in IDE
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.addReturnValueCheckerInfo()
            }
        }
    }

    jvm {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    js {
        if (!kotlinBuildProperties.isTeamcityBuild) {
            browser {}
        }
        nodejs {}
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.addAll(
                "-Xklib-ir-inliner=intra-module",
                "-Xir-module-name=$MODULE_NAME",
            )
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        (this as KotlinJsTargetDsl).compilerOptions {
            freeCompilerArgs.addAll(
                "-Xklib-ir-inliner=intra-module",
                "-source-map=false",
                "-source-map-embed-sources=",
            )
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$MODULE_NAME")
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        (this as KotlinJsTargetDsl).compilerOptions {
            freeCompilerArgs.addAll(
                "-Xklib-ir-inliner=intra-module",
                "-source-map=false",
                "-source-map-embed-sources=",
            )
        }
        compilations["main"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-Xir-module-name=$MODULE_NAME")
            compilerOptions.addReturnValueCheckerInfo()
        }
    }

    if (!kotlinBuildProperties.isInIdeaSync) {
        // Tier 1
        macosArm64()
        iosSimulatorArm64()
        iosArm64()

        // Tier 2
        linuxX64()
        linuxArm64()
        watchosSimulatorArm64()
        watchosArm32()
        watchosArm64()
        tvosSimulatorArm64()
        tvosArm64()

        // Tier 3
        androidNativeArm32()
        androidNativeArm64()
        androidNativeX86()
        androidNativeX64()
        mingwX64()
        watchosDeviceArm64()
        @Suppress("DEPRECATION") macosX64()
        @Suppress("DEPRECATION") iosX64()
        @Suppress("DEPRECATION") watchosX64()
        @Suppress("DEPRECATION") tvosX64()
    } else {
        // this magic is needed because of explicit dependency of common
        // source set on the stdlib
        when {
            HostManager.hostIsMac -> macosArm64("native")
            HostManager.hostIsMingw -> mingwX64("native")
            HostManager.hostIsLinux -> linuxX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

//publish()
//standardPublicJars()
