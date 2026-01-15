plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        browser()
        binaries.library()
    }

    sourceSets {
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}