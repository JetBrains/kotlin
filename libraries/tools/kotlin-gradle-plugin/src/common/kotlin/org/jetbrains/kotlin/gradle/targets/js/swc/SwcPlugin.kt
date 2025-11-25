/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask.Companion.deprecationMessage
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

abstract class SwcPlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createSwcRootEnvSpec()

        val settings = project.extensions.create(
            SwcExtension.EXTENSION_NAME,
            SwcExtension::class.java,
            project,
            spec
        )

        spec.initializeSwcRootEnvSpec(settings)

        addPlatform(project, settings)

        project.registerTask<SwcSetupTask>(
            JsPlatformDisambiguator.extensionName(SwcSetupTask.BASE_NAME),
            listOf(spec)
        ) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a swc"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        project.registerTask<CleanDataTask>(
            JsPlatformDisambiguator.extensionName(
                "swc" + CleanDataTask.NAME_SUFFIX,
                prefix = null,
            )
        ) {
            it.doFirst {
                it.logger.warn(deprecationMessage(it.path))
            }

            it.cleanableStoreProvider = spec
                .installationDirectory
                .map { CleanableStore.Companion[it.asFile.path] }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local swc version"
        }
    }

    private fun ExtensionContainer.createSwcRootEnvSpec(): SwcEnvSpec {
        return create(
            SwcEnvSpec.EXTENSION_NAME,
            SwcEnvSpec::class.java
        )
    }

    private fun SwcEnvSpec.initializeSwcRootEnvSpec(
        rootSwc: SwcExtension,
    ) {
        download.convention(rootSwc.downloadProperty)
        // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
        downloadBaseUrl.set(rootSwc.downloadBaseUrlProperty)
        allowInsecureProtocol.convention(false)
        installationDirectory.convention(rootSwc.installationDirectory)
        version.convention(rootSwc.versionProperty)
        command.convention(rootSwc.commandProperty)
        platform.convention(rootSwc.platform)
    }

    private fun addPlatform(project: Project, extension: SwcExtension) {
        extension.platform.value(
            project.providers.systemProperty("os.name")
                .zip(
                    project.providers.systemProperty("os.arch")
                ) { name, arch ->
                    SwcPlatform.parseSwcPlatform(name.toLowerCaseAsciiOnly(), arch)
                }
        ).disallowChanges()
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "swc"

        internal fun apply(rootProject: Project): SwcExtension {
            rootProject.plugins.apply(SwcPlugin::class.java)
            return rootProject.extensions.getByName(
                SwcExtension.EXTENSION_NAME
            ) as SwcExtension
        }

        internal val Project.kotlinSwcExtension: SwcExtension
            get() = extensions.getByName(
                SwcExtension.EXTENSION_NAME
            ).castIsolatedKotlinPluginClassLoaderAware()
    }
}
