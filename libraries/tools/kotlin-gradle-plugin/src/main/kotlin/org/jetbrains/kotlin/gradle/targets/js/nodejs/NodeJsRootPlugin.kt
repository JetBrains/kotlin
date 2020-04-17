/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension.Companion.EXTENSION_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask

open class NodeJsRootPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        project.plugins.apply(BasePlugin::class.java)

        check(project == project.rootProject) {
            "NodeJsRootPlugin can be applied only to root project"
        }

        val settings = this.extensions.create(EXTENSION_NAME, NodeJsRootExtension::class.java, this)

        val setupTask = tasks.create(NodeJsSetupTask.NAME, NodeJsSetupTask::class.java) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
        }

        val rootClean = project.rootProject.tasks.named(BasePlugin.CLEAN_TASK_NAME)

        tasks.register(KotlinNpmInstallTask.NAME, KotlinNpmInstallTask::class.java) {
            it.dependsOn(setupTask)
            it.group = TASKS_GROUP_NAME
            it.description = "Find, download and link NPM dependencies and projects"

            it.mustRunAfter(rootClean)
        }

        YarnPlugin.apply(project)

        tasks.register("node" + CleanDataTask.NAME_SUFFIX, CleanDataTask::class.java) {
            it.cleanableStoreProvider = provider { settings.requireConfigured().cleanableStore }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local node version"
        }
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(rootProject: Project): NodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(NodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(EXTENSION_NAME) as NodeJsRootExtension
        }
    }
}
