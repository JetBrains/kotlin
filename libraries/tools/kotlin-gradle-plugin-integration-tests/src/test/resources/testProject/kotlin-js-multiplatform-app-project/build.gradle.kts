plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}