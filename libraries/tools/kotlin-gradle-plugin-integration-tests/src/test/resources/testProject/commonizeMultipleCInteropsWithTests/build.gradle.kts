import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.konan.target.Family.*

operator fun KotlinSourceSet.invoke(builder: SourceSetHierarchyBuilder.() -> Unit): KotlinSourceSet {
    SourceSetHierarchyBuilder(this).builder()
    return this
}

class SourceSetHierarchyBuilder(private val node: KotlinSourceSet) {
    operator fun KotlinSourceSet.unaryMinus() = this.dependsOn(node)
}

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
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
    val commonTest = sourceSets.getByName("commonTest")
    val jvmMain = sourceSets.getByName("jvmMain")
    val nativeMain = sourceSets.create("nativeMain")
    val nativeTest = sourceSets.create("nativeTest")
    val unixMain = sourceSets.create("unixMain")
    val unixTest = sourceSets.create("unixTest")
    val linuxMain = sourceSets.create("linuxMain")
    val linuxTest = sourceSets.create("linuxTest")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxX64Test = sourceSets.getByName("linuxX64Test")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
    val linuxArm64Test = sourceSets.getByName("linuxArm64Test")
    val appleMain = sourceSets.create("appleMain")
    val appleTest = sourceSets.create("appleTest")
    val macosMain = sourceSets.getByName("macosMain")
    val macosTest = sourceSets.getByName("macosTest")
    val iosMain = sourceSets.create("iosMain")
    val iosX64Main = sourceSets.getByName("iosX64Main")
    val iosArm64Main = sourceSets.getByName("iosArm64Main")
    val iosSimulatorArm64Main = sourceSets.getByName("iosSimulatorArm64Main")
    val iosTest = sourceSets.create("iosTest")
    val iosX64Test = sourceSets.getByName("iosX64Test")
    val iosArm64Test = sourceSets.getByName("iosArm64Test")
    val iosSimulatorArm64Test = sourceSets.getByName("iosSimulatorArm64Test")
    val windowsX64Main = sourceSets.getByName("windowsX64Main")
    val windowsX64Test = sourceSets.getByName("windowsX64Test")

    commonMain {
        -jvmMain
        -nativeMain {
            -unixMain {
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

    commonTest {
        -nativeTest {
            -unixTest {
                -appleTest {
                    -iosTest {
                        -iosX64Test
                        -iosArm64Test
                        -iosSimulatorArm64Test
                    }
                    -macosTest
                }
                -linuxTest {
                    -linuxArm64Test
                    -linuxX64Test
                }
            }
            -windowsX64Test
        }
    }

    if (properties["testSourceSetsDependingOnMain"] == "true") {
        logger.quiet("testSourceSetsDependingOnMain is set")
        nativeTest.dependsOn(nativeMain)
        unixTest.dependsOn(unixMain)
        appleTest.dependsOn(appleMain)
        linuxTest.dependsOn(linuxMain)
    }

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName("main").cinterops.create("nativeHelper") {
            headers(file("libs/nativeHelper.h"))
        }

        target.compilations.getByName("test").cinterops.create("nativeTestHelper") {
            headers(file("libs/nativeTestHelper.h"))
        }

        if (target.konanTarget.family.isAppleFamily || target.konanTarget.family == LINUX) {
            target.compilations.getByName("main").cinterops.create("unixHelper") {
                headers(file("libs/unixHelper.h"))
            }
        }

        if (target.konanTarget.family.isAppleFamily) {
            target.compilations.getByName("main").cinterops.create("appleHelper") {
                headers(file("libs/appleHelper.h"))
            }
        }

        if (target.konanTarget.family == IOS) {
            target.compilations.getByName("test").cinterops.create("iosTestHelper") {
                headers(file("libs/iosTestHelper.h"))
            }
        }

        if (target.konanTarget.family == MINGW) {
            target.compilations.getByName("main").cinterops.create("windowsHelper") {
                headers(file("libs/windowsHelper.h"))
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
