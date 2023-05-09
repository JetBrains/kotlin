import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
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
                customField("customField1", mapOf("one" to 1, "two" to 2))
                customField("customField2", null)
                customField("customField3" to null)
                customField("customField4", mapOf("foo" to null))
            }
        }
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    val kotlinNodeJs = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()

    tasks.register<Exec>("runWebpackResult") {
        val webpackTask = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserProductionWebpack")
        dependsOn(webpackTask)

        executable(kotlinNodeJs.requireConfigured().nodeExecutable)

        workingDir(webpackTask.flatMap { it.outputDirectory.asFile })
        args("./${project.name}.js")
    }
}

tasks.named<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>("compileKotlinJs") {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
tasks.named<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>("compileTestKotlinJs") {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>().configureEach {
    kotlinOptions {
        moduleKind = "es"
    }
}
