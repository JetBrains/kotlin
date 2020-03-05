/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask

open class YarnPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        check(project == project.rootProject) {
            "YarnPlugin can be applied only to root project"
        }

        val nodeJs = NodeJsRootPlugin.apply(this)

        val yarnRootExtension = this.extensions.create(YarnRootExtension.YARN, YarnRootExtension::class.java, this)

        tasks.create(YarnSetupTask.NAME, YarnSetupTask::class.java) {
            it.dependsOn(nodeJs.nodeJsSetupTask)
        }

        tasks.register("yarn" + CleanDataTask.NAME_SUFFIX, CleanDataTask::class.java) {
            it.cleanableStoreProvider = provider { yarnRootExtension.requireConfigured().cleanableStore }
            it.description = "Clean unused local yarn version"
        }
    }

    companion object {
        fun apply(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YarnRootExtension.YARN) as YarnRootExtension
        }
    }
}
