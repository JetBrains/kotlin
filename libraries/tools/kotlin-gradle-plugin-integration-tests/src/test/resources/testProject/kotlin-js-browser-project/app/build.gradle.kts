import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import javax.inject.Inject

plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(project(":lib"))
    implementation(npm(projectDir.resolve("src/main/css")))
    testImplementation(kotlin("test-js"))
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
    target {
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

        compilations.named("main") {
            packageJson {
                customField("customField", mapOf("one" to 1, "two" to 2))
            }
        }
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    val kotlinNodeJs = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()

    tasks.register<Exec>("runWebpackResult") {
        dependsOn(tasks.named("browserProductionWebpack"))

        executable(kotlinNodeJs.requireConfigured().nodeExecutable)

        workingDir = File("${buildDir}").resolve("distributions")
        args("./${project.name}.js")
    }
}