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
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "9"
        }
    }
    js("a") {
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
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val aMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val aTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}