/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir

@ExperimentalWasmDsl
abstract class D8Plugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createD8EnvSpec(project)

        project.registerTask<D8SetupTask>(
            WasmPlatformDisambiguator.extensionName(D8SetupTask.BASE_NAME),
            listOf(spec)
        ) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a D8"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        @Suppress("DEPRECATION")
        project.registerTask<org.jetbrains.kotlin.gradle.tasks.CleanDataTask>(
            WasmPlatformDisambiguator.extensionName(
                "d8" + org.jetbrains.kotlin.gradle.tasks.CleanDataTask.NAME_SUFFIX,
                prefix = null,
            )
        ) {
            it.doFirst {
                it.logger.warn(org.jetbrains.kotlin.gradle.tasks.CleanDataTask.Companion.deprecationMessage(it.path))
            }

            it.cleanableStoreProvider = spec
                .installationDirectory
                .map { org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore.Companion[it.asFile.path] }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local d8 version"
        }
    }

    private fun ExtensionContainer.createD8EnvSpec(project: Project): D8EnvSpec {
        return create(
            D8EnvSpec.EXTENSION_NAME,
            D8EnvSpec::class.java
        ).apply {
            val kotlinUserDir = project.userKotlinPersistentDir

            download.convention(true)
            // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
            downloadBaseUrl.set("https://storage.googleapis.com/chromium-v8/official/canary")
            allowInsecureProtocol.convention(false)
            installationDirectory.convention(
                project.objects.directoryProperty().fileValue(kotlinUserDir.resolve("d8"))
            )
            version.convention("13.4.61")
            edition.convention("rel")
            command.convention("d8")
        }
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "d8"

        internal fun applyWithEnvSpec(project: Project): D8EnvSpec {
            project.plugins.apply(D8Plugin::class.java)
            return project.extensions.getByName(
                D8EnvSpec.EXTENSION_NAME
            ) as D8EnvSpec
        }
    }
}