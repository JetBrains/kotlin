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
}

kotlin {
    js()
    jvm()

    linuxX64()
    linuxArm64()

    macosX64("macos")
    ios()

    mingwX64("windowsX64")
    mingwX86("windowsX86")

    val commonMain by sourceSets.getting
    val concurrentMain by sourceSets.creating
    val jvmMain by sourceSets.getting
    val jsMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val appleAndLinuxMain by sourceSets.creating
    val linuxMain by sourceSets.creating
    val linuxX64Main by sourceSets.getting
    val linuxArm64Main by sourceSets.getting
    val appleMain by sourceSets.creating
    val macosMain by sourceSets.getting
    val iosMain by sourceSets.getting
    val windowsMain by sourceSets.creating
    val windowsX64Main by sourceSets.getting
    val windowsX86Main by sourceSets.getting

    commonMain {
        -jsMain
        -concurrentMain {
            -jvmMain
            -nativeMain {
                -appleAndLinuxMain {
                    -appleMain {
                        -iosMain
                        -macosMain
                    }
                    -linuxMain {
                        -linuxArm64Main
                        -linuxX64Main
                    }
                }
                -windowsMain {
                    -windowsX64Main
                    -windowsX86Main
                }
            }
        }
    }


    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
    }

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName("main").cinterops.create("withPosix") {
            header(file("libs/withPosix.h"))
        }
    }
}
