import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsLinux
import org.jetbrains.kotlin.konan.target.KonanTarget.LINUX_MIPS32

plugins {
    kotlin("multiplatform")
}

kotlin {
    presets.forEach { preset ->
        if (preset is AbstractKotlinNativeTargetPreset) {
            targetFromPreset(preset)
        }
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
