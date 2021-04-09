/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask

open class YarnPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "YarnPlugin can be applied only to root project"
        }

        val yarnRootExtension = this.extensions.create(YarnRootExtension.YARN, YarnRootExtension::class.java, this)
        val nodeJs = NodeJsRootPlugin.apply(this)

        registerTask<YarnSetupTask>(YarnSetupTask.NAME) {
            it.dependsOn(nodeJs.nodeJsSetupTaskProvider)
        }

        val rootClean = project.rootProject.tasks.named(BasePlugin.CLEAN_TASK_NAME)

        val rootPackageJson = tasks.register(RootPackageJsonTask.NAME, RootPackageJsonTask::class.java) { task ->
            task.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            task.description = "Create root package.json"

            task.mustRunAfter(rootClean)
        }

        tasks.named(KotlinNpmInstallTask.NAME).configure {
            it.dependsOn(rootPackageJson)
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
