package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension.Companion.EXTENSION_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

open class NodeJsRootPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        project.plugins.apply(BasePlugin::class.java)

        check(project == project.rootProject) {
            "NodeJsRootPlugin can be applied only to root project"
        }

        this.extensions.create(EXTENSION_NAME, NodeJsRootExtension::class.java, this)

        val setupTask = tasks.create(NodeJsSetupTask.NAME, NodeJsSetupTask::class.java) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
        }

        tasks.create(KotlinNpmInstallTask.NAME, KotlinNpmInstallTask::class.java) {
            it.dependsOn(setupTask)
            it.group = TASKS_GROUP_NAME
            it.description = "Find, download and link NPM dependencies and projects"
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
