plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(project(":lib"))
                testImplementation(kotlin("test-js"))
            }
        }
    }
}

kotlin {
    js {
        browser {
            testTask {
                enabled = false
            }
        }
    }
}
