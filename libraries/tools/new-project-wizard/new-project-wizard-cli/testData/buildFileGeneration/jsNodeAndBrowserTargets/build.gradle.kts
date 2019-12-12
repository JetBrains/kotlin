plugins {
    kotlin("multiplatform") version "1.3.61"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    js("nodeJs") {
        nodejs {

        }
    }
    js("browser") {
        browser {

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
        val browserTest by getting
    }
}