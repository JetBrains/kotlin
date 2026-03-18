import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform") apply true
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    val nativePlatform = when {
        HostManager.hostIsMac -> macosX64("nativePlatform")
        HostManager.hostIsLinux -> linuxX64("nativePlatform")
        HostManager.hostIsMingw -> mingwX64("nativePlatform")
        else -> throw IllegalStateException("Unsupported host")
    }

    val commonMain by sourceSets.getting
    val nativePlatformMain by sourceSets.getting
    val nativeMain by sourceSets.creating

    nativeMain.dependsOn(commonMain)
    nativePlatformMain.dependsOn(nativeMain)

    nativePlatform.compilations.getByName("main").cinterops.create("dummy") {
        headers("libs/include/dummy.h")
        compilerOpts.add("-Ilibs/include")
    }
}