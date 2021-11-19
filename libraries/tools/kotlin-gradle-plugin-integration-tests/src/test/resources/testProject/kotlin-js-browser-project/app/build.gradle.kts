plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(project(":lib"))
    implementation(npm(projectDir.resolve("src/main/css")))
    testImplementation(kotlin("test-js"))
}

kotlin {
    target {
        browser {
            webpackTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
                enabled = false // Task is disabled because it requires browser to be installed. That may be a problem on CI.
                                // Disabled but configured task allows us to check at least a part of configuration cache correctness.
            }
        }
        binaries.executable()

        compilations.named("main") {
            packageJson {
                customField("customField", mapOf("one" to 1, "two" to 2))
            }
        }
    }
}