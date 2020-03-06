plugins {
    kotlin("multiplatform") version "1.3.61"
}
group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}