package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension.Companion.NODE_JS
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver

open class NodeJsPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        check(project == project.rootProject) {
            "NodeJsPlugin can be applied only to root project"
        }

        val nodeJs = this.extensions.create(NODE_JS, NodeJsRootExtension::class.java, this)
        tasks.create(NodeJsSetupTask.NAME, NodeJsSetupTask::class.java)

//        setupCleanNodeModulesTask(project, nodeJs)

        allprojects {
            if (it != project) {
                it.extensions.create(NODE_JS, NodeJsExtension::class.java, this)
            }
        }
    }

    private fun setupCleanNodeModulesTask(
        project: Project,
        nodeJs: NodeJsRootExtension
    ) {
        project.tasks.create("cleanNodeModules", Delete::class.java) {
            it.description = "Deletes node_modules and package.json file"
            it.group = BasePlugin.BUILD_GROUP

            val npmProjectLayout = NpmProjectLayout[it.project]

            project.delete(npmProjectLayout.nodeModulesDir)
            project.afterEvaluate {
                if (nodeJs.manageNodeModules) {
                    project.delete(npmProjectLayout.packageJsonFile)
                }
            }

            project.tasks.maybeCreate(BasePlugin.CLEAN_TASK_NAME).dependsOn(it)
        }
    }

    companion object {
        operator fun get(project: Project): NodeJsRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(NodeJsPlugin::class.java)
            return rootProject.extensions.getByName(NODE_JS) as NodeJsRootExtension
        }
    }
}
