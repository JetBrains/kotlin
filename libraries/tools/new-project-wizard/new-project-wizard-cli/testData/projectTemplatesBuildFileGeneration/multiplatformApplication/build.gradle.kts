plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("KOTLIN_REPO") }
}

kotlin {
    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}