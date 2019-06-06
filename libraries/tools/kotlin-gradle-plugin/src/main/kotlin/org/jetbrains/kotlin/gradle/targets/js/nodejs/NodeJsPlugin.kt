package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension.Companion.EXTENSION_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolveTask

open class NodeJsPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        check(project == project.rootProject) {
            "NodeJsPlugin can be applied only to root project"
        }

        this.extensions.create(EXTENSION_NAME, NodeJsRootExtension::class.java, this)

        val setupTask = tasks.create(NodeJsSetupTask.NAME, NodeJsSetupTask::class.java) {
            it.group = TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
        }

        tasks.create(NpmResolveTask.NAME, NpmResolveTask::class.java) {
            it.dependsOn(setupTask)
            it.outputs.upToDateWhen { false }
            it.group = TASKS_GROUP_NAME
            it.description = "Find, download and link NPM dependencies and projects"
        }

        setupCleanNodeModulesTask(project)

        allprojects {
            if (it != project) {
                it.extensions.create(EXTENSION_NAME, NodeJsExtension::class.java, this)
            }
        }
    }

    private fun setupCleanNodeModulesTask(project: Project) {
        project.tasks.create("cleanKotlinNodeModules", Delete::class.java) {
            it.description = "Deletes nodeJs projects created during build"
            it.group = BasePlugin.BUILD_GROUP
            it.delete.add(project.nodeJs.root.rootPackageDir)
        }

        project.tasks.create("cleanKotlinGradleNodeModules", Delete::class.java) {
            it.description = "Deletes node modules imported from gradle external modules"
            it.group = BasePlugin.BUILD_GROUP
            it.delete.add(project.nodeJs.root.nodeModulesGradleCacheDir)
        }
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(project: Project): NodeJsExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(NodeJsPlugin::class.java)
            return rootProject.extensions.getByName(EXTENSION_NAME) as NodeJsExtension
        }
    }
}
