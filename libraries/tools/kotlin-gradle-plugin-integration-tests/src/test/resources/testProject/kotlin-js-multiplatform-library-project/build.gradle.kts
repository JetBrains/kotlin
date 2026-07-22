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
        val jsTest = getByName("jsTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}