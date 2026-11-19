import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    // "native" keeps task names the same on every CI host.
    val native = hostNativeTarget("native") {
        binaries.executable()
        compilations.getByName("main").cinterops.create("sample") {
            header(file("src/nativeInterop/cinterop/sample.h"))
        }
    } ?: error("Unexpected host: ${nativeHost.name}")

    // Declared everywhere, linkable only on macOS.
    iosArm64 {
        compilations.getByName("main").cinterops.create("sample") {
            header(file("src/nativeInterop/cinterop/sample.h"))
        }
    }

    if (nativeHost.isMacOs) {
        iosSimulatorArm64()
    }

    // Printed for the test.
    logger.quiet("nativeHost.name=${nativeHost.name}")
    logger.quiet("nativeHost.targetName=${nativeHost.targetName}")
    logger.quiet("hostNativeTarget.name=${native.name}")
}

// Capture the host here, not in a top-level val: a script property captured by a task lambda
// cannot be stored in the configuration cache.
tasks.withType<CInteropProcess>().configureEach {
    val host = KotlinNativeHost.current()
    inputs.property("kotlin.native.host", host.name)
    onlyIf("host cannot link ${konanTarget.name}") { host.canLinkBinaries(konanTarget.name) }
}
