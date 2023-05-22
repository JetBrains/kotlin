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
    ios()
    mingwX64("windowsX64")
    @Suppress("DEPRECATION_ERROR")
    mingwX86("windowsX86")

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
    val iosMain by sourceSets.getting
    val iosTest by sourceSets.getting
    val windowsMain by sourceSets.creating
    val windowsTest by sourceSets.creating
    val windowsX64Main by sourceSets.getting
    val windowsX64Test by sourceSets.getting
    val windowsX86Main by sourceSets.getting
    val windowsX86Test by sourceSets.getting

    commonMain {
        -jvmMain
        -nativeMain {

            /*
            Different from p1&p2:
            - Does not include macos
            - Does not include linuxArm64
             */
            -appleAndLinuxMain {
                -iosMain
                -linuxX64Main
            }

            /*
            Different from p1&p2:
            - A source set with those targets only exists here

            Expected to see p1:nativeMain cinterops
            */
            -windowsAndLinuxMain {
                -windowsMain
                -linuxX64Main
            }

            -windowsMain {
                -windowsX64Main
                -windowsX86Main
            }
        }
    }

    commonTest {
        -nativeTest {
            -appleAndLinuxTest {
                -iosTest
                -linuxX64Test
            }

            -windowsAndLinuxTest {
                -windowsTest
                -linuxX64Test
            }

            -windowsTest {
                -windowsX64Test
                -windowsX86Test
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
