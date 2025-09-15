import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        binaries.executable()
        browser {
            webpackTask {
                generateConfigOnly = true
            }
            runTask {
                generateConfigOnly = true
            }
        }
        val nodeJs = project.rootProject.kotlinNodeJsRootExtension
        val npmToolingDest = nodeJs.toolingInstallTaskProvider.flatMap { it.destination }
        val modules = NpmProjectModules(npmToolingDest.get().asFile)
        val compilation = compilations.getByName("main")
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigDevelopmentWebpack") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("wasmJsBrowserDevelopmentWebpack")
            args("configtest")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("wasmJsBrowserDevelopmentWebpack").flatMap { it.configFile }
            environment("KOTLIN_TOOLING_DIR", modules.dir.resolve("node_modules"))
            environment("NODE_PATH", modules.dir.resolve("node_modules"))

            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigProductionWebpack") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("wasmJsBrowserProductionWebpack")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("wasmJsBrowserProductionWebpack").flatMap { it.configFile }
            environment("KOTLIN_TOOLING_DIR", modules.dir.resolve("node_modules"))
            environment("NODE_PATH", modules.dir.resolve("node_modules"))

            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigDevelopmentRun") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("wasmJsBrowserDevelopmentRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("wasmJsBrowserDevelopmentRun").flatMap { it.configFile }
            args("configtest")
            environment("KOTLIN_TOOLING_DIR", modules.dir.resolve("node_modules"))
            environment("NODE_PATH", modules.dir.resolve("node_modules"))

            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigProductionRun") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("wasmJsBrowserProductionRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("wasmJsBrowserProductionRun").flatMap { it.configFile }
            args("configtest")
            environment("KOTLIN_TOOLING_DIR", modules.dir.resolve("node_modules"))
            environment("NODE_PATH", modules.dir.resolve("node_modules"))

            doFirst {
                args(configFile.get().absolutePath)
            }
        }
    }

    js {
        binaries.executable()
        browser {
            webpackTask {
                generateConfigOnly = true
            }
            runTask {
                generateConfigOnly = true
            }
        }
        val compilation = compilations.getByName("main")
        val modules = NpmProjectModules(compilation.npmProject.dir.get().asFile)
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigDevelopmentWebpack") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("jsBrowserDevelopmentWebpack")
            args("configtest")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("jsBrowserDevelopmentWebpack").flatMap { it.configFile }

            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigProductionWebpack") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("jsBrowserProductionWebpack")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("jsBrowserProductionWebpack").flatMap { it.configFile }

            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigDevelopmentRun") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("jsBrowserDevelopmentRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("jsBrowserDevelopmentRun").flatMap { it.configFile }
            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
        org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec.register(compilation, "${this.name}CheckConfigProductionRun") {
            inputFileProperty.set(provider { modules.require("webpack/bin/webpack.js") }.map { RegularFile { File(it) } })
            dependsOn("jsBrowserProductionRun")
            val configFile = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("jsBrowserProductionRun").flatMap { it.configFile }
            args("configtest")
            doFirst {
                args(configFile.get().absolutePath)
            }
        }
    }
}