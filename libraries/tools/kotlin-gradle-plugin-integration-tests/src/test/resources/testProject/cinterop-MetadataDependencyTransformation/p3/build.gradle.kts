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

    val commonMain = sourceSets.getByName("commonMain")
    val commonTest = sourceSets.getByName("commonTest")
    val jvmMain = sourceSets.getByName("jvmMain")
    val nativeMain = sourceSets.create("nativeMain")
    val nativeTest = sourceSets.create("nativeTest")
    val appleAndLinuxMain = sourceSets.create("appleAndLinuxMain")
    val appleAndLinuxTest = sourceSets.create("appleAndLinuxTest")
    val windowsAndLinuxMain = sourceSets.create("windowsAndLinuxMain")
    val windowsAndLinuxTest = sourceSets.create("windowsAndLinuxTest")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxX64Test = sourceSets.getByName("linuxX64Test")
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
