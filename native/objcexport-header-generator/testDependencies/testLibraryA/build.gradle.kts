import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
}


tasks.register("prepareDependencyKlib") {
    val target = when (HostManager.host) {
        KonanTarget.MACOS_X64 -> kotlin.macosX64()
        KonanTarget.MACOS_ARM64 -> kotlin.macosArm64()
        KonanTarget.LINUX_X64 -> kotlin.linuxX64()
        KonanTarget.LINUX_ARM64 -> kotlin.linuxArm64()
        else -> return@register
    }

    val mainCompilation = target.compilations["main"]
    dependsOn(mainCompilation.compileTaskProvider)
}
