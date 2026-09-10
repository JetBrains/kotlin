plugins {
    kotlin("multiplatform")
}

abstract class CustomWebpackRule
@javax.inject.Inject
constructor(name: String) : org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule(name) {
    init {
        test.set("none")
    }
    override fun loaders() = listOf<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule.Loader>()
}

kotlin {
    wasmJs {
        browser {
            webpackTask {
                cssSupport {
                    enabled.set(true)
                }
                scssSupport {
                    enabled.set(true)
                }
                rules {
                    rule<CustomWebpackRule>("custom")
                }
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
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(npm(projectDir.resolve("src/wasmJsMain/css")))
            }
        }
    }
}
