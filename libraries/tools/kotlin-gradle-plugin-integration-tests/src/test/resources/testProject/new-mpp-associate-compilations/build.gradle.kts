import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    val commonIntegrationTest by sourceSets.creating

    jvm {
        compilations["test"].defaultSourceSet.dependencies {
            implementation(kotlin("test-junit"))
        }
    }

    js {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    mingwX64("mingw64")
    linuxX64("linux64")
    macosX64("macos64")
    macosArm64("macosArm64")
    iosX64("iosX64")
    iosSimulatorArm64("iosSimulatorArm64")

    targets.matching { it.name != "metadata" }.all target@{
        val testCompilation = this@target.compilations["test"]

        compilations.create("integrationTest") {
            associateWith(testCompilation)
            defaultSourceSet.dependsOn(commonIntegrationTest)
        }
    }


    testableTargets.all {
        testRuns {
            named("test") {
                filter {
                    excludeTestsMatching("*.secondTest")
                }
            }
            create("integration") {
                filter {
                    includeTestsMatching("com.example.HelloIntegrationTest.test")
                }
            }
        }
    }

    jvm {
        testRuns {
            named("test") {
                filter {
                    excludeTestsMatching("com.example.HelloTest.secondTest")
                }
            }
            named("integration") {
                setExecutionSourceFrom(compilations["integrationTest"])
            }
        }
    }

    // TODO KT-68454 custom test runs for JS don't work
    //js {
    //    binaries.executable(js().compilations["integrationTest"])
    //    testRuns {
    //        named("test") {
    //            filter {
    //                excludeTestsMatching("com.example.HelloTest.secondTest")
    //            }
    //        }
    //        named("integration") {
    //            setExecutionSourceFrom(compilations["integrationTest"])
    //        }
    //    }
    //}

    targets.withType<KotlinNativeTargetWithTests<*>>().all {
        binaries {
            test("integration", listOf(DEBUG)) {
                compilation = compilations["integrationTest"]
            }
        }
        testRuns {
            named("test") {
                filter {
                    excludeTestsMatching("*.secondTest")
                }
            }
            named("integration") {
                setExecutionSourceFrom(binaries.getTest("integration", "DEBUG"))
            }
        }
    }
}
