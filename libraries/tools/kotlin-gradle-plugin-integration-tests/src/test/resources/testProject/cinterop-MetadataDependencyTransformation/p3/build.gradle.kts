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
    val windowsAndLinuxMain by sourceSets.creating
    val windowsAndLinuxTest by sourceSets.creating
    val linuxX64Main by sourceSets.getting
    val linuxX64Test by sourceSets.getting
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

            /*
            Different from p1&p2:
            - Does not include macos
            - Does not include linuxArm64
             */
            -appleAndLinuxMain {
                -iosMain {
                    -iosX64Main
                    -iosArm64Main
                    -iosSimulatorArm64Main
                }
                -linuxX64Main
            }

            /*
            Different from p1&p2:
            - A source set with those targets only exists here

            Expected to see p1:nativeMain cinterops
            */
            -windowsAndLinuxMain {
                -windowsX64Main
                -linuxX64Main
            }
        }
    }

    commonTest {
        -nativeTest {
            -appleAndLinuxTest {
                -iosTest {
                    -iosX64Test
                    -iosArm64Test
                    -iosSimulatorArm64Test
                }
                -linuxX64Test
            }

            -windowsAndLinuxTest {
                -windowsX64Test
                -linuxX64Test
            }
        }
    }

    sourceSets.commonMain.get().dependencies {
        implementation(project(":p2"))
    }

    sourceSets.all {
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
