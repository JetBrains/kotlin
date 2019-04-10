/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

open class YarnPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        check(project == project.rootProject) {
            "YarnPlugin can be applied only to root project"
        }

        this.extensions.create(YarnRootExtension.YARN, YarnRootExtension::class.java, this)
        tasks.create(YarnSetupTask.NAME, YarnSetupTask::class.java) {
            it.dependsOn(NodeJsPlugin[this].nodeJsSetupTask)
        }
    }

    companion object {
        operator fun get(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YarnRootExtension.YARN) as YarnRootExtension
        }
    }
}
