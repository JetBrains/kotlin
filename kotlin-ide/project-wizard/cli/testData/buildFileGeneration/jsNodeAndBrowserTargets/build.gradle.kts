plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}
kotlin {
    js("nodeJs") {
        nodejs {
            binaries.executable()
        }
    }
    js("browser") {
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
        val nodeJsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val nodeJsTest by getting
        val browserMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val browserTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}