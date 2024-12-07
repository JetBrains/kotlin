plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val jvmWithoutJava = jvm("jvmWithoutJava")
    val js = js {
        nodejs()
    }
    val macos64 = macosX64("macos64")
    val macosArm64 = macosArm64("macosArm64")
    val linux64 = linuxX64("linux64")
    val mingw64 = mingwX64("mingw64")

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvmWithoutJava.compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("script-runtime"))
            }
        }

        jvmWithoutJava.compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        js.compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        configure(listOf(macos64, macosArm64, linux64, mingw64)) {
            compilations["main"].defaultSourceSet.dependsOn(nativeMain)
        }
    }
}
