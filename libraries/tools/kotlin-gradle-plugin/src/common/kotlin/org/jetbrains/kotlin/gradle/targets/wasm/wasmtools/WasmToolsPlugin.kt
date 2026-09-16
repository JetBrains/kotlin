/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask

@ExperimentalWasmDsl
abstract class WasmToolsPlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)

        val spec = project.createWasmToolsEnvSpec()

        project.registerTask<WasmToolsSetupTask>(
            WasmPlatformDisambiguator.extensionName(WasmToolsSetupTask.BASE_NAME),
            listOf(spec)
        ) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install wasm-tools"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    private fun Project.createWasmToolsEnvSpec(): WasmToolsEnvSpec {
        return extensions.create(
            WasmToolsEnvSpec.EXTENSION_NAME,
            WasmToolsEnvSpec::class.java
        ).apply {
            val gradleHome = gradle.gradleUserHomeDir.let {
                project.logger.kotlinInfo("Storing cached files in $it")
                objects.directoryProperty().fileValue(it)
            }

            download.convention(true)
            // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
            downloadBaseUrl.set("https://github.com/bytecodealliance/wasm-tools/releases/download")
            allowInsecureProtocol.convention(false)
            installationDirectory.convention(
                gradleHome.dir("wasm-tools")
            )
            version.convention("1.259.0")
            command.convention("wasm-tools")
        }
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "wasm-tools"

        internal fun applyWithEnvSpec(project: Project): WasmToolsEnvSpec {
            project.plugins.apply(WasmToolsPlugin::class.java)
            return project.extensions.getByName(
                WasmToolsEnvSpec.EXTENSION_NAME
            ) as WasmToolsEnvSpec
        }
    }
}
