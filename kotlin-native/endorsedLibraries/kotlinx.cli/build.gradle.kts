import org.jetbrains.kotlin.*

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlin-stdlib-common"))
            }
            kotlin.srcDir("src/main/kotlin")
            compilerOptions {
                freeCompilerArgs.add("-Xname-based-destructuring=complete")
            }
        }
        commonTest {
            dependencies {
                // projectOrFiles is required for the performance project that includes kotlinx.cli compositely
                projectOrFiles(project, ":kotlin-test")?.let { implementation(it) }
            }
            kotlin.srcDir("src/tests")
            compilerOptions {
                freeCompilerArgs.add("-Xname-based-destructuring=complete")
            }
        }
        jvm {
            compilations["main"].defaultSourceSet {
                dependencies {
                    implementation(project(":kotlin-stdlib-jdk8"))
                }
                kotlin.srcDir("src/main/kotlin-jvm")
            }
            // JVM-specific tests and their dependencies:
            compilations["test"].defaultSourceSet {
                dependencies {
                    implementation(kotlinTest("junit"))
                }
            }

            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        optIn.add("kotlinx.cli.ExperimentalCli")
                        suppressWarnings = true
                    }
                }
            }
        }
    }
}
