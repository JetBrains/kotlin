import org.jetbrains.kotlin.*

plugins {
    id("common-configuration")
    id("test-inputs-check")
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
        }
        commonTest {
            dependencies {
                implementation(project(":kotlin-test"))
            }
            kotlin.srcDir("src/tests")
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
                    implementation(kotlinTest("junit5"))
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
    compilerOptions {
        freeCompilerArgs.addAll(dogfoodedExperimentalFeatures)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
