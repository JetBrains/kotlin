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
        val jsTest = getByName("jsTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}