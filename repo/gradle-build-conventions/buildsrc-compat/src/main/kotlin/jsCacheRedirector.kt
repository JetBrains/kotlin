/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.npm.exec.NpmExecRunner
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.variant.VariantComputer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

const val NPM_REGISTRY_CACHE = "https://cache-redirector.jetbrains.com/registry.npmjs.org"

fun Project.configureJsCacheRedirector() {
    pluginManager.withPlugin("com.github.node-gradle.node") {
        tasks.withType<NpmTask>().configureEach {
            val command = npmCommand.orNull?.takeIf { it.isNotEmpty() }
                ?: args.get() // some tasks may be configured by putting command into args instead of npmCommand
            if (command.firstOrNull() in listOf("install", "ci")) {
                val workingDirectory = workingDir.orNull?.asFile ?: layout.projectDirectory.asFile
                val npmRcFile = workingDirectory.resolve(".npmrc")

                outputs.file(npmRcFile)

                doFirst {
                    logger.info("Setting Npm registry for $path to $NPM_REGISTRY_CACHE")
                    val nodeExecConfiguration =
                        NodeExecConfiguration(
                            listOf("config", "set", "registry", NPM_REGISTRY_CACHE, "--location=project"),
                            environment.get(),
                            workingDir.asFile.orNull,
                            ignoreExitValue.get(),
                            execOverrides.orNull
                        )
                    val npmExecRunner = objects.newInstance(NpmExecRunner::class.java)
                    npmExecRunner.executeNpmCommand(projectHelper, nodeExtension, nodeExecConfiguration, VariantComputer())
                }
            }
        }
    }

    if (project.parent == null) {
        plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
            val nodeJsRoot = the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()
            nodeJsRoot.npmInstallTaskProvider.configureNpmRegistry()
        }

        plugins.withType(org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin::class.java) {
            val wasmNodeJsRoot = the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension>()
            wasmNodeJsRoot.npmInstallTaskProvider.configureNpmRegistry()
        }
    }
}

private fun TaskProvider<out KotlinNpmInstallTask>.configureNpmRegistry() {
    configure {
        if ("--registry" !in args) {
            args += listOf("--registry", NPM_REGISTRY_CACHE)
        }
    }
}
