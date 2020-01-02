plugins {
    kotlin("multiplatform") version "1.3.61"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    jvm()
    js("a") {
        browser {

        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting
        val aMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val aTest by getting
    }
}