/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parsePlatform
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import kotlin.reflect.KClass

internal class NodeJsPluginApplier(
    private val platformDisambiguate: HasPlatformDisambiguate,
    private val nodeJsEnvSpecKlass: KClass<out AbstractNodeJsEnvSpec>,
    private val nodeJsEnvSpecName: String,
    private val nodeJsRootApply: (project: Project) -> AbstractNodeJsRootExtension,
) {

    fun apply(project: Project) {
        MultiplePluginDeclarationDetector.Companion.detect(project)

        val nodeJs = project.createNodeJsEnvSpec(nodeJsEnvSpecKlass, nodeJsEnvSpecName) {
            nodeJsRootApply(project.rootProject)
        }

        project.registerTask<NodeJsSetupTask>(platformDisambiguate.extensionName(NodeJsSetupTask.Companion.NAME), listOf(nodeJs)) {
            it.group = NodeJsRootPlugin.Companion.TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Project.createNodeJsEnvSpec(
        nodeJsEnvSpecKlass: KClass<out AbstractNodeJsEnvSpec>,
        nodeJsEnvSpecName: String,
        nodeJsConstructor: () -> AbstractNodeJsRootExtension,
    ): AbstractNodeJsEnvSpec {
        val extensions = extensions
        val objects = objects

        return extensions.create(
            nodeJsEnvSpecName,
            nodeJsEnvSpecKlass.java,
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

    private fun addPlatform(project: Project, extension: AbstractNodeJsEnvSpec) {
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
}