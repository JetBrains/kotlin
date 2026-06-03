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

val dependencyMode = providers.gradleProperty("dependencyMode")

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
    val appleAndLinuxMain = sourceSets.create("appleAndLinuxMain")
    val appleAndLinuxTest = sourceSets.create("appleAndLinuxTest")
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
    val iosTest = sourceSets.create("iosTest")
    val iosX64Main = sourceSets.getByName("iosX64Main")
    val iosArm64Main = sourceSets.getByName("iosArm64Main")
    val iosSimulatorArm64Main = sourceSets.getByName("iosSimulatorArm64Main")
    val iosX64Test = sourceSets.getByName("iosX64Test")
    val iosArm64Test = sourceSets.getByName("iosArm64Test")
    val iosSimulatorArm64Test = sourceSets.getByName("iosSimulatorArm64Test")
    val windowsX64Main = sourceSets.getByName("windowsX64Main")
    val windowsX64Test = sourceSets.getByName("windowsX64Test")

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
        when (dependencyMode.getOrNull()) {
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
                api("kotlin-multiplatform-projects:p1:1.0.0")
            }
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
