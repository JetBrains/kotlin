/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention

open class NodeJsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        val nodeJs = project.createNodeJsEnvSpec {
            NodeJsRootPlugin.apply(project.rootProject)
        }

        project.registerTask<NodeJsSetupTask>(NodeJsSetupTask.NAME, listOf(nodeJs)) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Project.createNodeJsEnvSpec(
        nodeJsConstructor: () -> NodeJsRootExtension,
    ): NodeJsEnvSpec {
        val extensions = extensions
        val objects = objects

        return extensions.create(
            NodeJsEnvSpec.EXTENSION_NAME,
            NodeJsEnvSpec::class.java
        ).apply {
            installationDirectory.convention(
                objects.directoryProperty().fileProvider(
                    objects.providerWithLazyConvention {
                        nodeJsConstructor().installationDir
                    }
                )
            )
            download.convention(objects.providerWithLazyConvention { nodeJsConstructor().download })
            // set instead of convention because it is possible to have null value
            downloadBaseUrl.set(objects.providerWithLazyConvention { nodeJsConstructor().downloadBaseUrl })
            allowInsecureProtocol.convention(false)
            version.convention(objects.providerWithLazyConvention { nodeJsConstructor().version })
            command.convention(objects.providerWithLazyConvention { nodeJsConstructor().command })

            addPlatform(this@createNodeJsEnvSpec, this)
        }
    }

    private fun addPlatform(project: Project, extension: NodeJsEnvSpec) {
        val uname = project.providers
            .unameExecResult

        extension.platform.value(
            project.providers.systemProperty("os.name")
                .zip(
                    project.providers.systemProperty("os.arch")
                ) { name, arch ->
                    parsePlatform(name, arch, uname)
                }
        ).disallowChanges()
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(project: Project): NodeJsEnvSpec {
            project.plugins.apply(NodeJsPlugin::class.java)
            return project.extensions.getByName(NodeJsEnvSpec.EXTENSION_NAME) as NodeJsEnvSpec
        }

        val Project.kotlinNodeJsEnvSpec: NodeJsEnvSpec
            get() = extensions.getByName(NodeJsEnvSpec.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}
