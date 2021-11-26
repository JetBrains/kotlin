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
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserDevelopmentWebpack").map { it.configFile }.get()
            args("configtest")
            args(configFile.absolutePath)
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigProductionWebpack") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserProductionWebpack")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserProductionWebpack").map { it.configFile }.get()
            args("configtest")
            args(configFile.absolutePath)
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigDevelopmentRun") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserDevelopmentRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserDevelopmentRun").map { it.configFile }.get()
            args("configtest")
            args(configFile.absolutePath)
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.create(compilation, "checkConfigProductionRun") {
            inputFileProperty.set(provider { compilation.npmProject.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("browserProductionRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserProductionRun").map { it.configFile }.get()
            args("configtest")
            args(configFile.absolutePath)
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