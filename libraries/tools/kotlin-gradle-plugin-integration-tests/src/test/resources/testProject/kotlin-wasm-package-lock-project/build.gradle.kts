plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        useCommonJs()
        binaries.executable()
        nodejs {
        }
        browser {
            // CI doesn't have browser, so that's why we have to disable test task execution
            testTask {
                enabled = false
            }
        }
    }
}