import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.native.tasks.artifact.*
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

kotlinArtifact("mylib", Library) {
    target = LINUX_X64
}

kotlinArtifact("myslib", Library) {
    target = LINUX_X64
    modes = setOf(NativeBuildType.DEBUG)
    addModule(project(":lib"))
}

kotlinArtifact(XCFramework) {
    targets = setOf(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64)
    setModules(
        project(":shared"),
        project(":lib")
    )
}