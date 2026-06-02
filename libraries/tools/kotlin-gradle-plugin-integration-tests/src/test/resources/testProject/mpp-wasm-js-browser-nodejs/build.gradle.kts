plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        browser {
        }
        nodejs {
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsTest = getByName("wasmJsTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}