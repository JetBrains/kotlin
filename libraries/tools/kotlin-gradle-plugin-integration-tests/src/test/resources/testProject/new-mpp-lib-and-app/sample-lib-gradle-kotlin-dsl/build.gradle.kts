plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
    //id("org.jetbrains.kotlin.plugin.allopen")
    //id("org.jetbrains.kotlin.plugin.noarg")
    //id("com.gradleup.shadow")
    //id("application")
    //id("org.jetbrains.kotlin.kapt")
}

group = "com.example"
version = "1.0"

val shouldBeJs = true

kotlin {
    // Check the new preset functions:
    jvm("jvm6") { }

    if (shouldBeJs) {
        js("nodeJs") {
            nodejs()
        }
        wasmJs()
    }

    linuxX64("linux64")
    mingwX64("mingw64")
    macosX64("macos64")
    macosArm64("macosArm64")

    targets {
        all {
            mavenPublication {
                pom.withXml {
                    asNode().appendNode("name", "Sample MPP library")
                }
            }
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvm6Main by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
            }
        }
        val jvm6Test by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val nativeMain by creating { dependsOn(commonMain.get()) }
        val linux64Main by getting { dependsOn(nativeMain) }
        val macos64Main by getting { dependsOn(nativeMain) }
        val macosArm64Main by getting { dependsOn(nativeMain) }
    }
}

kotlin.sourceSets.forEach { println(it.kotlin.srcDirs) }

// Check that a compilation may be created after project evaluation, KT-28896:
afterEvaluate {
    kotlin {
        jvm("jvm6").compilations.create("benchmark") {
            tasks.named("assemble") { dependsOn(compileKotlinTask) }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri("<localRepo>")
        }
    }
}
