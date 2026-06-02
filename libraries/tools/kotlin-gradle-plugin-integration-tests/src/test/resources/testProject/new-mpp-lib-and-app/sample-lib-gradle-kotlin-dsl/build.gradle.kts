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
        val jvm6Main = getByName("jvm6Main") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.23.4")
            }
        }
        val jvm6Test = getByName("jvm6Test") {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val nativeMain = create("nativeMain") { dependsOn(commonMain.get()) }
        val linux64Main = getByName("linux64Main") { dependsOn(nativeMain) }
        val macos64Main = getByName("macos64Main") { dependsOn(nativeMain) }
        val macosArm64Main = getByName("macosArm64Main") { dependsOn(nativeMain) }
    }
}

kotlin.sourceSets.forEach { println(it.kotlin.srcDirs) }

// Check that a compilation may be created after project evaluation, KT-28896:
afterEvaluate {
    kotlin {
        jvm("jvm6").compilations.create("benchmark") {
            tasks.named("assemble") { dependsOn(compileTaskProvider) }
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
