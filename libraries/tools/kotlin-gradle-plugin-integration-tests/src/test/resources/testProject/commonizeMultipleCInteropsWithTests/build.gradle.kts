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
    ios()

    mingwX64("windowsX64")

    val commonMain by sourceSets.getting
    val commonTest by sourceSets.getting
    val jvmMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val nativeTest by sourceSets.creating
    val unixMain by sourceSets.creating
    val unixTest by sourceSets.creating
    val linuxMain by sourceSets.creating
    val linuxTest by sourceSets.creating
    val linuxX64Main by sourceSets.getting
    val linuxX64Test by sourceSets.getting
    val linuxArm64Main by sourceSets.getting
    val linuxArm64Test by sourceSets.getting
    val appleMain by sourceSets.creating
    val appleTest by sourceSets.creating
    val macosMain by sourceSets.getting
    val macosTest by sourceSets.getting
    val iosMain by sourceSets.getting
    val iosTest by sourceSets.getting
    val windowsX64Main by sourceSets.getting
    val windowsX64Test by sourceSets.getting

    commonMain {
        -jvmMain
        -nativeMain {
            -unixMain {
                -appleMain {
                    -iosMain
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
                    -iosTest
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
