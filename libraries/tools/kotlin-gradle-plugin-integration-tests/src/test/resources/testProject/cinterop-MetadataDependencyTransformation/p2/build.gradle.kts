import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

operator fun KotlinSourceSet.invoke(builder: SourceSetHierarchyBuilder.() -> Unit): KotlinSourceSet {
    SourceSetHierarchyBuilder(this).builder()
    return this
}

class SourceSetHierarchyBuilder(private val node: KotlinSourceSet) {
    operator fun KotlinSourceSet.unaryMinus() = this.dependsOn(node)
}

repositories {
    maven {
        url = rootProject.buildDir.resolve("repo").toURI()
    }
}

plugins {
    kotlin("multiplatform")
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
    val iosMain by sourceSets.getting
    val iosTest by sourceSets.getting
    val windowsX64Main by sourceSets.getting
    val windowsX64Test by sourceSets.getting

    commonMain {
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

            -windowsX64Main
        }
    }

    commonTest {
        -nativeTest {
            -appleAndLinuxTest {
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
