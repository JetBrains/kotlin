import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

version = "1.0.0"

publishing {
    repositories {
        maven("<localRepo>")
    }
}

kotlin {
    mingwX64()
    val targetsWithInterop = listOf(linuxX64(), linuxArm64())

    targets.withType<KotlinNativeTarget>().forEach { target ->
        if (!HostManager().isEnabled(target.konanTarget)) {
            error("Expected all targets to be supported. ${target.konanTarget} is disabled on this host!")
        }
    }

    val commonMain = sourceSets.getByName("commonMain")
    val withInteropMain = sourceSets.create("withInteropMain")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")

    withInteropMain.dependsOn(commonMain)
    linuxX64Main.dependsOn(withInteropMain)
    linuxArm64Main.dependsOn(withInteropMain)

    targetsWithInterop.forEach { target ->
        target.compilations.getByName("main").cinterops.create("withPosix") {
            this.packageName = "withPosix"
            header(file("libs/withPosix.h"))
        }
        target.compilations.getByName("main").cinterops.create("simple") {
            header(file("libs/simple.h"))
        }
    }
}
