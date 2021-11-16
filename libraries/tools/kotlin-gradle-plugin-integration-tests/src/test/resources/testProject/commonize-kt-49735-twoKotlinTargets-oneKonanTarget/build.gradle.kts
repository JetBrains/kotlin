import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    val platformTargetA = when {
        HostManager.hostIsMac -> macosX64("platformA")
        HostManager.hostIsMingw -> mingwX64("platformA")
        HostManager.hostIsLinux -> linuxX64("platformA")
        else -> error("Unexpected host: ${HostManager.host}")
    }

    val platformTargetB = when {
        HostManager.hostIsMac -> macosX64("platformB")
        HostManager.hostIsMingw -> mingwX64("platformB")
        HostManager.hostIsLinux -> linuxX64("platformB")
        else -> error("Unexpected host: ${HostManager.host}")
    }
}
