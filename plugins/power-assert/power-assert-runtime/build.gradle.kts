import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
}

description = "Kotlin Power-Assert Runtime"

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=full",
            "-Xallow-kotlin-package",
        )
    }

    metadata() // For common sources in IDE

    jvm()

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        // This is required because of the common source set dependency on a local stdlib.
        // Only these targets are added in the stdlib project during IDEA sync.
        when {
            HostManager.hostIsMac -> @Suppress("DEPRECATION") macosX64("native")
            HostManager.hostIsMingw -> mingwX64("native")
            HostManager.hostIsLinux -> linuxX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
    } else {
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
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlinStdlib())
        }
        commonTest.dependencies {
            api(kotlinTest())
        }
        jvmTest.dependencies {
            api(kotlinTest("junit"))
        }
    }
}
