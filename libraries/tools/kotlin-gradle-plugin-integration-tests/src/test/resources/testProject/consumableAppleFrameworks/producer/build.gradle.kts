import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

kotlin {
    macosX64 {
        binaries.framework("macosX64Specific") {  }
    }
    macosArm64 {
        binaries.framework("macosArm64Specific") {  }
    }
    iosX64()
    iosArm64()

    // Add to all targets two more frameworks
    targets.filterIsInstance<KotlinNativeTarget>().forEach {
        it.binaries.framework("static", listOf(NativeBuildType.RELEASE)) { isStatic = true }
        it.binaries.framework("dynamic", listOf(NativeBuildType.DEBUG)) { isStatic = false }
    }
}