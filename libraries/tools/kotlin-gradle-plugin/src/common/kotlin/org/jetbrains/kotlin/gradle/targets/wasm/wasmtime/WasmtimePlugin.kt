/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtime

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask

@ExperimentalWasmDsl
abstract class WasmtimePlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)

        val spec = project.createWasmtimeEnvSpec()

        project.registerTask<WasmtimeSetupTask>(
            WasmPlatformDisambiguator.extensionName(WasmtimeSetupTask.BASE_NAME),
            listOf(spec)
        ) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install Wasmtime"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        @Suppress("DEPRECATION_ERROR")
        project.registerTask<org.jetbrains.kotlin.gradle.tasks.CleanDataTask>(
            WasmPlatformDisambiguator.extensionName(
                "wasmtime" + org.jetbrains.kotlin.gradle.tasks.CleanDataTask.NAME_SUFFIX,
                prefix = null,
            )
        ) {}
    }

    private fun Project.createWasmtimeEnvSpec(): WasmtimeEnvSpec {
        return extensions.create(
            WasmtimeEnvSpec.EXTENSION_NAME,
            WasmtimeEnvSpec::class.java
        ).apply {
            val gradleHome = gradle.gradleUserHomeDir.let {
                project.logger.kotlinInfo("Storing cached files in $it")
                objects.directoryProperty().fileValue(it)
            }

            download.convention(true)
            // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
            downloadBaseUrl.set("https://github.com/bytecodealliance/wasmtime/releases/download")
            allowInsecureProtocol.convention(false)
            installationDirectory.convention(
                gradleHome.dir("wasmtime")
            )
            version.convention("46.0.1")
            command.convention("wasmtime")
        }
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "wasmtime"

        internal fun applyWithEnvSpec(project: Project): WasmtimeEnvSpec {
            project.plugins.apply(WasmtimePlugin::class.java)
            return project.extensions.getByName(
                WasmtimeEnvSpec.EXTENSION_NAME
            ) as WasmtimeEnvSpec
        }
    }
}
