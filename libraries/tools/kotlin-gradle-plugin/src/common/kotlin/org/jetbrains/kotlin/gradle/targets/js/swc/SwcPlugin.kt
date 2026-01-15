/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal abstract class SwcPlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createSwcEnvSpec()
        spec.initializeSwcRootEnvSpec(project)

        project.registerTask<SwcSetupTask>(
            JsPlatformDisambiguator.extensionName(SwcSetupTask.BASE_NAME),
            listOf(spec)
        ) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install swc."
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    private fun ExtensionContainer.createSwcEnvSpec(): SwcEnvSpec {
        return create(
            SwcEnvSpec.EXTENSION_NAME,
            SwcEnvSpec::class.java
        )
    }

    private fun SwcEnvSpec.initializeSwcRootEnvSpec(
        project: Project,
    ) {
        val gradleHome = project.gradle.gradleUserHomeDir.also {
            project.logger.kotlinInfo("Storing cached files in $it")
        }
        download.convention(true)
        // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
        downloadBaseUrl.set("https://github.com/swc-project/swc/releases/download")
        allowInsecureProtocol.convention(false)
        installationDirectory.fileValue(gradleHome.resolve("swc"))
        version.convention("1.15.3")
        command.convention("compile")
        platform.convention(
            project.providers.systemProperty("os.name")
                .zip(
                    project.providers.systemProperty("os.arch")
                ) { name, arch ->
                    SwcPlatform.parseSwcPlatform(name.toLowerCaseAsciiOnly(), arch)
                }
        )
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "swc"

        internal fun apply(project: Project): SwcEnvSpec {
            project.plugins.apply(SwcPlugin::class.java)
            return project.extensions.getByName(
                SwcEnvSpec.EXTENSION_NAME
            ) as SwcEnvSpec
        }
    }
}
