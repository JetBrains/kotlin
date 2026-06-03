import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

operator fun KotlinSourceSet.invoke(builder: SourceSetHierarchyBuilder.() -> Unit): KotlinSourceSet {
    SourceSetHierarchyBuilder(this).builder()
    return this
}

class SourceSetHierarchyBuilder(private val node: KotlinSourceSet) {
    operator fun KotlinSourceSet.unaryMinus() = this.dependsOn(node)
}

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
    jvm()

    linuxX64()
    linuxArm64()

    macosX64("macos")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    mingwX64("windowsX64")

    val commonMain = sourceSets.getByName("commonMain")
    val concurrentMain = sourceSets.create("concurrentMain")
    val jvmMain = sourceSets.getByName("jvmMain")
    val nativeMain = sourceSets.create("nativeMain")
    val appleAndLinuxMain = sourceSets.create("appleAndLinuxMain")
    val linuxMain = sourceSets.create("linuxMain")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
    val appleMain = sourceSets.create("appleMain")
    val macosMain = sourceSets.getByName("macosMain")
    val iosMain = sourceSets.create("iosMain")
    val iosX64Main = sourceSets.getByName("iosX64Main")
    val iosArm64Main = sourceSets.getByName("iosArm64Main")
    val iosSimulatorArm64Main = sourceSets.getByName("iosSimulatorArm64Main")
    val windowsX64Main = sourceSets.getByName("windowsX64Main")

    commonMain {
        -concurrentMain {
            -jvmMain
            -nativeMain {
                -appleAndLinuxMain {
                    -appleMain {
                        -iosMain {
                            -iosX64Main
                            -iosArm64Main
                            -iosSimulatorArm64Main
                        }
                        -macosMain
                    }
                    -linuxMain {
                        -linuxArm64Main
                        -linuxX64Main
                    }
                }

                -windowsX64Main
            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main").cinterops.create("withPosix") {
            this.packageName = "withPosix"
            header(file("libs/withPosix.h"))
        }
        compilations.getByName("main").cinterops.create("simple") {
            header(file("libs/simple.h"))
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
