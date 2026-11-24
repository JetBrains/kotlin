import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}


kotlin {
    val nativeTarget = when {
        HostManager.hostIsMac -> macosArm64("native")
        HostManager.hostIsMingw -> mingwX64("native")
        HostManager.host == KonanTarget.LINUX_ARM64 -> linuxArm64("native")
        HostManager.hostIsLinux -> linuxX64("native")
        else -> error("Unexpected host: ${HostManager.host}")
    }

    nativeTarget.compilations["main"].cinterops.create("sampleInterop") {
        header(file("src/nativeInterop/cinterop/sampleInterop.h"))
        header(file("src/nativeInterop/cinterop/sampleInteropNoise.h"))
    }
}
