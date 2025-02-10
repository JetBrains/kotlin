/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

@ExperimentalWasmDsl
abstract class D8Plugin internal constructor() :
    @Suppress("DEPRECATION")
    org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin() {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        val spec = project.extensions.createD8EnvSpec()

        if (project == project.rootProject) {
            project.extensions.create(
                D8RootExtension.EXTENSION_NAME,
                D8RootExtension::class.java,
                project,
                spec
            )
        }

        val d8RootExtension = applyRootProject(project.rootProject)

        spec.initializeD8EnvSpec(d8RootExtension)

        project.registerTask<D8SetupTask>(D8SetupTask.NAME, listOf(spec)) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a D8"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        project.registerTask<CleanDataTask>("d8" + CleanDataTask.NAME_SUFFIX) {
            it.cleanableStoreProvider = spec.env.map { it.cleanableStore }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local d8 version"
        }
    }

    private fun ExtensionContainer.createD8EnvSpec(): D8EnvSpec {
        return create(
            D8EnvSpec.EXTENSION_NAME,
            D8EnvSpec::class.java
        )
    }

    private fun D8EnvSpec.initializeD8EnvSpec(
        d8: D8RootExtension,
    ) {
        download.convention(d8.downloadProperty)
        downloadBaseUrl.convention(d8.downloadBaseUrlProperty)
        allowInsecureProtocol.convention(false)
        installationDirectory.convention(d8.installationDirectory)
        version.convention(d8.versionProperty)
        edition.convention(d8.edition)
        command.convention(d8.commandProperty)
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "d8"

        @InternalKotlinGradlePluginApi
        internal fun apply(project: Project): D8RootExtension {
            project.plugins.apply(D8Plugin::class.java)
            return project.extensions.getByName(D8RootExtension.EXTENSION_NAME) as D8RootExtension
        }

        internal fun applyWithEnvSpec(project: Project): D8EnvSpec {
            project.plugins.apply(D8Plugin::class.java)
            return project.extensions.getByName(D8EnvSpec.EXTENSION_NAME) as D8EnvSpec
        }

        private fun applyRootProject(project: Project): D8RootExtension {
            project.rootProject.plugins.apply(D8Plugin::class.java)
            return project.rootProject.extensions.getByName(D8RootExtension.EXTENSION_NAME) as D8RootExtension
        }

        @InternalKotlinGradlePluginApi
        internal val Project.kotlinD8RootExtension: D8RootExtension
            get() = extensions.getByName(D8RootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}