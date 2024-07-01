/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

open class NodeJsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        val nodeJs = project.extensions.create(
            NodeJsExtension.EXTENSION_NAME,
            NodeJsExtension::class.java,
            project
        )

        addPlatform(project, nodeJs)

        project.registerTask<NodeJsSetupTask>(NodeJsSetupTask.NAME) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
            it.configuration = project.provider {
                project.configurations.detachedConfiguration(project.dependencies.create(it.ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    private fun addPlatform(project: Project, extension: NodeJsExtension) {
        val uname = project.variantImplementationFactory<UnameExecutor.UnameExecutorVariantFactory>()
            .getInstance(project)
            .unameExecResult

        extension.platform.value(
            project.providers.systemProperty("os.name")
                .usedAtConfigurationTime(project.configurationTimePropertiesAccessor)
                .zip(
                    project.providers.systemProperty("os.arch")
                        .usedAtConfigurationTime(project.configurationTimePropertiesAccessor)
                ) { name, arch ->
                    parsePlatform(name, arch, uname)
                }
        ).disallowChanges()
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(project: Project): NodeJsExtension {
            project.plugins.apply(NodeJsPlugin::class.java)
            return project.extensions.getByName(NodeJsExtension.EXTENSION_NAME) as NodeJsExtension
        }

        val Project.kotlinNodeJsExtension: NodeJsExtension
            get() = extensions.getByName(NodeJsExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}
