import gradle.kotlin.dsl.accessors._c314c1502f0fcd9ad0c9fc692c470a48.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Used in for modules in 'native/objcexport-heade-generator/testDependencies.
 * Such libraries can build klibs that can then later be used for running objc export tests against
 */
plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
}

/*
Build the klib for the current host platfrom and put it at a given, predictable
location, so that the test utils can later pick this klib up from this known location.
 */
tasks.register<Copy>("prepareTestKlib") {
    val target = when (HostManager.host) {
        KonanTarget.MACOS_X64 -> kotlin.macosX64()
        KonanTarget.MACOS_ARM64 -> kotlin.macosArm64()
        KonanTarget.LINUX_X64 -> kotlin.linuxX64()
        KonanTarget.LINUX_ARM64 -> kotlin.linuxArm64()
        else -> return@register
    }

    val mainCompilation = target.compilations["main"]
    dependsOn(mainCompilation.compileTaskProvider)

    into(layout.buildDirectory.file("testKlib"))
    from(mainCompilation.compileTaskProvider.map { it.outputFile }) {
        rename { "${project.name}.klib" }
    }
}
