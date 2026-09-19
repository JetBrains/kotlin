/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.parsePlatform
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import kotlin.reflect.KClass

/**
 * Responsible for applying Node.js specific plugin configurations to a project.
 * This class integrates the Node.js environment management functionality
 * and sets up related tasks and configurations tied to a given project.
 *
 * @constructor Creates an instance of NodeJsPluginApplier.
 * @param platformDisambiguate Interface providing additional platform-specific naming or disambiguation logic for the applied plugin.
 * @param nodeJsEnvSpecKlass The class type of the NodeJs environment specification to be created.
 * @param nodeJsEnvSpecName The name of the NodeJs environment specification configuration to be registered.
 * @param nodeJsRootApply A function that provides the root NodeJs environment extension in the project.
 */
internal class NodeJsPluginApplier(
    private val platformDisambiguate: HasPlatformDisambiguator,
    private val nodeJsEnvSpecKlass: KClass<out BaseNodeJsEnvSpec>,
    private val nodeJsEnvSpecName: String,
    private val nodeJsRootApply: (project: Project) -> BaseNodeJsRootExtension,
) {

    fun apply(project: Project) {
        val nodeJs = project.createNodeJsEnvSpec(nodeJsEnvSpecKlass, nodeJsEnvSpecName)

        project.registerTask<NodeJsSetupTask>(platformDisambiguate.extensionName(NodeJsSetupTask.BASE_NAME), listOf(nodeJs)) {
            it.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    @Suppress("DEPRECATION_ERROR")
    private fun Project.createNodeJsEnvSpec(
        nodeJsEnvSpecKlass: KClass<out BaseNodeJsEnvSpec>,
        nodeJsEnvSpecName: String,
    ): BaseNodeJsEnvSpec {
        val extensions = extensions
        val objects = objects

        return extensions.create(
            nodeJsEnvSpecName,
            nodeJsEnvSpecKlass.java,
        ).apply {

            installationDirectory.convention(
                objects.directoryProperty().fileValue(
                    gradle.gradleUserHomeDir.also {
                        logger.kotlinInfo("Storing cached files in $it")
                    }
                )
            )
            download.convention(true)
            // set instead of convention because it is possible to have null value https://github.com/gradle/gradle/issues/14768
            downloadBaseUrl.set("https://nodejs.org/dist")
            allowInsecureProtocol.convention(false)
            version.convention("24.16.0")
            command.convention("node")

            addPlatform(this@createNodeJsEnvSpec, this)
        }
    }

    private fun addPlatform(project: Project, extension: BaseNodeJsEnvSpec) {
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
