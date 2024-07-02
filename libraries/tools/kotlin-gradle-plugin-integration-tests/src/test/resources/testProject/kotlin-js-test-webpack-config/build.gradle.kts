import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        val compilation = compilations.getByName("main")
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigDevelopmentWebpack") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserDevelopmentWebpack")
            args("configtest")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserDevelopmentWebpack").flatMap { it.configFile }

            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigProductionWebpack") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserProductionWebpack")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserProductionWebpack").flatMap { it.configFile }

            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigDevelopmentRun") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserDevelopmentRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserDevelopmentRun").flatMap { it.configFile }
            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigProductionRun") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserProductionRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserProductionRun").flatMap { it.configFile }
            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        binaries.executable()
        browser {
            webpackTask {
                generateConfigOnly = true
            }
            runTask {
                generateConfigOnly = true
            }
        }
    }
}