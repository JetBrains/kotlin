plugins {
    kotlin("js")
}

val kotlinVersion: String by extra("1.8.20-RC")

repositories {
    mavenCentral()
}

kotlin {
    js(LEGACY) {
        browser {
            binaries.executable()
            distribution {
                directory = project.file("js")
            }
            compilations.all {
                compilerOptions.configure {
                    freeCompilerArgs.set(listOf(
                            "-Xmulti-platform",
                            "-opt-in=kotlin.js.ExperimentalJsExport"
                    ))
                }
            }
        }
    }

    sourceSets["main"].kotlin {
        srcDir("../../benchmarks/shared/src")
        srcDir("../shared/src/main/kotlin")
        srcDir("../src/main/kotlin-js")
    }
}
