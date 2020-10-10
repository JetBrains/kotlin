plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}

group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("KOTLIN_REPO")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "9"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js("a", LEGACY) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    sourceSets {
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val aMain by getting
        val aTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}