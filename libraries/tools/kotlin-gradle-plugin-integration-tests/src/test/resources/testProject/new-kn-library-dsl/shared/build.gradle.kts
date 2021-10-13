import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.library
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.nativeLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
}

//:shared:assembleDebugSharedMylibLinuxX64
nativeLibrary("mylib") {
    targets = listOf(LINUX_X64)
    artifact = library
    from(project(":shared"))
}

//:shared:assembleDebugStaticMyslibLinuxX64
nativeLibrary("myslib") {
    targets = listOf(LINUX_X64)
    artifact = library
    modes = setOf(DEBUG)
    isStatic = true
    from(
        project(":shared"),
        project(":lib")
    )
}

