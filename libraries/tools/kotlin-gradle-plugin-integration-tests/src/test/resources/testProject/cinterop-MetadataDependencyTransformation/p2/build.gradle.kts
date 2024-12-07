import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

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
    jvm()

    linuxX64()
    linuxArm64()

    macosX64("macos")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    mingwX64("windowsX64")

    val commonMain by sourceSets.getting
    val commonTest by sourceSets.getting
    val jvmMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val nativeTest by sourceSets.creating
    val appleAndLinuxMain by sourceSets.creating
    val appleAndLinuxTest by sourceSets.creating
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
    val iosMain by sourceSets.creating
    val iosTest by sourceSets.creating
    val iosX64Main by sourceSets.getting
    val iosArm64Main by sourceSets.getting
    val iosSimulatorArm64Main by sourceSets.getting
    val iosX64Test by sourceSets.getting
    val iosArm64Test by sourceSets.getting
    val iosSimulatorArm64Test by sourceSets.getting
    val windowsX64Main by sourceSets.getting
    val windowsX64Test by sourceSets.getting

    commonMain {
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

    commonTest {
        -nativeTest {
            -appleAndLinuxTest {
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

    sourceSets.commonMain.get().dependencies {
        when (project.properties["dependencyMode"]?.toString()) {
            null -> {
                logger.warn("dependencyMode = null -> Using 'project'")
                api(project(":p1"))
            }

            "project" -> {
                logger.quiet("dependencyMode = 'project'")
                api(project(":p1"))
            }

            "repository" -> {
                logger.quiet("dependencyMode = 'repository'")
                api("kotlin-multiplatform-projects:p1:1.0.0-SNAPSHOT")
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
