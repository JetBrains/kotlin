/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenPlatform.Companion.parseBinaryenPlatform
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExtension.Companion.EXTENSION_NAME
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@OptIn(ExperimentalWasmDsl::class)
abstract class BinaryenPlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createBinaryenEnvSpec()

        val settings = project.extensions.create(
            EXTENSION_NAME,
            BinaryenExtension::class.java,
            project,
            spec
        )

        spec.initializeBinaryenRootEnvSpec(settings)

        addPlatform(project, settings)

        project.registerTask<BinaryenSetupTask>(BinaryenSetupTask.NAME, listOf(spec)) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a binaryen"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        project.registerTask<CleanDataTask>("binaryen" + CleanDataTask.NAME_SUFFIX) {
            it.cleanableStoreProvider = project.provider { settings.requireConfigured().cleanableStore }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local binaryen version"
        }
    }

    private fun ExtensionContainer.createBinaryenEnvSpec(): BinaryenEnvSpec {
        return create(
            BinaryenEnvSpec.EXTENSION_NAME,
            BinaryenEnvSpec::class.java
        )
    }

    private fun BinaryenEnvSpec.initializeBinaryenRootEnvSpec(
        rootBinaryen: BinaryenExtension,
    ) {
        download.convention(rootBinaryen.downloadProperty)
        downloadBaseUrl.convention(rootBinaryen.downloadBaseUrlProperty)
        allowInsecureProtocol.convention(false)
        installationDirectory.convention(rootBinaryen.installationDirectory)
        version.convention(rootBinaryen.versionProperty)
        command.convention(rootBinaryen.commandProperty)
        platform.convention(rootBinaryen.platform)
    }

    private fun addPlatform(project: Project, extension: BinaryenExtension) {
        val uname = project.providers.unameExecResult

        extension.platform.value(
            project.providers.systemProperty("os.name")
                .zip(
                    project.providers.systemProperty("os.arch")
                ) { name, arch ->
                    parseBinaryenPlatform(name.toLowerCaseAsciiOnly(), arch, uname)
                }
        ).disallowChanges()
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "binaryen"

        fun apply(project: Project): BinaryenExtension {
            project.plugins.apply(BinaryenPlugin::class.java)
            return project.extensions.getByName(EXTENSION_NAME) as BinaryenExtension
        }

        val Project.kotlinBinaryenExtension: BinaryenExtension
            get() = extensions.getByName(EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}
