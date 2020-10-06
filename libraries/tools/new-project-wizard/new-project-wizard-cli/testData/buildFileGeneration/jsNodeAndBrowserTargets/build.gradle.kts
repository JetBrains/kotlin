plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}

group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("KOTLIN_REPO")
    }
}

kotlin {
    js("nodeJs", LEGACY) {
        nodejs {
            binaries.executable()
        }
    }
    js("browser", LEGACY) {
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
        val nodeJsMain by getting
        val nodeJsTest by getting
        val browserMain by getting
        val browserTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}