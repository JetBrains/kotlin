/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@ExperimentalWasmDsl
abstract class BinaryenPlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createBinaryenEnvSpec(project)

        project.registerTask<BinaryenSetupTask>(
            WasmPlatformDisambiguator.extensionName(
                BinaryenSetupTask.BASE_NAME,
            ),
            listOf(spec)
        ) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a binaryen"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        @Suppress("DEPRECATION")
        project.registerTask<org.jetbrains.kotlin.gradle.tasks.CleanDataTask>(
            WasmPlatformDisambiguator.extensionName(
                "binaryen" + org.jetbrains.kotlin.gradle.tasks.CleanDataTask.NAME_SUFFIX,
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
            it.description = "Clean unused local binaryen version"
        }
    }

    private fun ExtensionContainer.createBinaryenEnvSpec(project: Project): BinaryenEnvSpec {
        return create(
            BinaryenEnvSpec.EXTENSION_NAME,
            BinaryenEnvSpec::class.java
        ).apply {
            val kotlinUserDir = project.userKotlinPersistentDir

            download.convention(true)
            // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
            downloadBaseUrl.set("https://github.com/WebAssembly/binaryen/releases/download")
            allowInsecureProtocol.convention(false)
            installationDirectory.convention(
                project.objects.directoryProperty().fileValue(kotlinUserDir.resolve("binaryen"))
            )
            version.convention("123")
            command.convention("wasm-opt")

            addPlatform(project, this)
        }
    }

    private fun addPlatform(project: Project, envSpec: BinaryenEnvSpec) {
        val uname = project.providers.unameExecResult

        envSpec.platform.value(
            project.providers.systemProperty("os.name")
                .zip(
                    project.providers.systemProperty("os.arch")
                ) { name, arch ->
                    BinaryenPlatform.parseBinaryenPlatform(name.toLowerCaseAsciiOnly(), arch, uname)
                }
        ).disallowChanges()
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "binaryen"

        internal fun applyWithEnvSpec(project: Project): BinaryenEnvSpec {
            project.plugins.apply(BinaryenPlugin::class.java)
            return project.extensions.getByName(
                BinaryenEnvSpec.EXTENSION_NAME
            ) as BinaryenEnvSpec
        }
    }
}