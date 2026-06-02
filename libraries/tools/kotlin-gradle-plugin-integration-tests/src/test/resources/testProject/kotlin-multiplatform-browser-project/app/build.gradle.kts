plugins {
    kotlin("multiplatform")
}
kotlin {
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain = getByName("jsMain") {
            dependencies {
                implementation(project(":lib"))
                implementation(npm(projectDir.resolve("src/jsMain/css")))
            }
        }
        val jsTest = getByName("jsTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
