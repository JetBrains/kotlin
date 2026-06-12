plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        browser ()
    }

    sourceSets {
        val wasmJsTest = getByName("wasmJsTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
