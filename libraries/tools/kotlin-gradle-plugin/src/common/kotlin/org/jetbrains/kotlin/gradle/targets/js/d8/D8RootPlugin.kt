/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension.Companion.EXTENSION_NAME
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask

open class D8RootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        check(project == project.rootProject) {
            "D8RootPlugin can be applied only to root project"
        }

        val settings = project.extensions.create(EXTENSION_NAME, D8RootExtension::class.java, project)

        val downloadTask = project.registerTask<Download>("${TASKS_GROUP_NAME}Download") {
            val env = settings.requireConfigured()
            it.group = TASKS_GROUP_NAME
            it.src(env.downloadUrl)
            it.dest(env.zipPath)
            it.overwrite(false)
            it.description = "Download local d8 version"
        }

        project.registerTask<Copy>(INSTALL_TASK_NAME) {
            val env = settings.requireConfigured()
            it.onlyIf { env.zipPath.exists() && !env.executablePath.exists() }
            it.group = TASKS_GROUP_NAME
            it.from(project.zipTree(env.zipPath))
            it.into(env.targetPath)
            it.dependsOn(downloadTask)
            it.description = "Install local d8 version"
        }

        project.registerTask<CleanDataTask>("d8" + CleanDataTask.NAME_SUFFIX) {
            it.cleanableStoreProvider = project.provider { settings.requireConfigured().cleanableStore }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local d8 version"
        }
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "d8"
        const val INSTALL_TASK_NAME: String = "${TASKS_GROUP_NAME}Install"

        fun apply(rootProject: Project): D8RootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(D8RootPlugin::class.java)
            return rootProject.extensions.getByName(EXTENSION_NAME) as D8RootExtension
        }
    }
}
