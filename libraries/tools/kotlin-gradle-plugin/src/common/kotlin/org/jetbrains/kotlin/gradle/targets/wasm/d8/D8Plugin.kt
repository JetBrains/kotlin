/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.internal.jsToolingProject
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.toolchains.DEFAULT_D8_EDITION
import org.jetbrains.kotlin.gradle.toolchains.DEFAULT_D8_VERSION
import org.jetbrains.kotlin.gradle.toolchains.d8Toolchain

@ExperimentalWasmDsl
abstract class D8Plugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createD8EnvSpec()

        if (!d8Toolchain.isDisabled(project)) {
            // The D8 toolchain provisions D8 once for the whole build, so neither the root project extension
            // nor the setup task is needed here. Not touching the root project is what makes the D8 tooling
            // compatible with Isolated Projects.
            spec.initializeD8EnvSpec(project)
            return
        }

        if (project == project.jsToolingProject()) {
            @Suppress("DEPRECATION_ERROR")
            project.extensions.create(
                D8RootExtension.EXTENSION_NAME,
                D8RootExtension::class.java,
                project,
                spec
            )
        }

        val d8RootExtension = applyRootProject(project)

        spec.initializeD8EnvSpec(d8RootExtension)

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
    }

    private fun ExtensionContainer.createD8EnvSpec(): D8EnvSpec {
        return create(
            D8EnvSpec.EXTENSION_NAME,
            D8EnvSpec::class.java
        )
    }

    @Suppress("DEPRECATION_ERROR")
    private fun D8EnvSpec.initializeD8EnvSpec(
        d8: D8RootExtension,
    ) {
        download.convention(d8.downloadProperty)
        // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
        downloadBaseUrl.set(d8.downloadBaseUrlProperty)
        allowInsecureProtocol.convention(false)
        installationDirectory.convention(d8.installationDirectory)
        version.convention(d8.versionProperty)
        edition.convention(d8.edition)
        command.convention(d8.commandProperty)
    }

    /**
     * Initializes the spec without reaching out to the root project, for the case when the D8 toolchain
     * provisions D8 and the spec itself is not used to resolve the executable.
     */
    private fun D8EnvSpec.initializeD8EnvSpec(project: Project) {
        download.convention(false)
        allowInsecureProtocol.convention(false)
        installationDirectory.convention(
            project.objects.directoryProperty().fileValue(project.gradle.gradleUserHomeDir.resolve("d8"))
        )
        version.convention(DEFAULT_D8_VERSION)
        edition.convention(DEFAULT_D8_EDITION)
        command.convention("d8")
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "d8"

        internal fun applyWithEnvSpec(project: Project): D8EnvSpec {
            project.plugins.apply(D8Plugin::class.java)
            return project.extensions.getByName(
                D8EnvSpec.EXTENSION_NAME
            ) as D8EnvSpec
        }

        @Suppress("DEPRECATION_ERROR")
        private fun applyRootProject(project: Project): D8RootExtension {
            project.jsToolingProject().plugins.apply(D8Plugin::class.java)
            return project.jsToolingProject().extensions.getByName(
                D8RootExtension.EXTENSION_NAME
            ) as D8RootExtension
        }
    }
}
