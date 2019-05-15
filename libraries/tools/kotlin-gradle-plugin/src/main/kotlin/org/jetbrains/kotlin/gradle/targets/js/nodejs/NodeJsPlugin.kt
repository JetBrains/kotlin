package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension.Companion.NODE_JS
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolveTask
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

open class NodeJsPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        check(project == project.rootProject) {
            "NodeJsPlugin can be applied only to root project"
        }

        this.extensions.create(NODE_JS, NodeJsRootExtension::class.java, this)

        val setupTask = tasks.create(NodeJsSetupTask.NAME, NodeJsSetupTask::class.java)
        val npmResolveTask = tasks.create(NpmResolveTask.NAME, NpmResolveTask::class.java)
        npmResolveTask.outputs.upToDateWhen { false }

        npmResolveTask.dependsOn(setupTask)

        setupCleanNodeModulesTask(project)

        allprojects {
            if (it != project) {
                it.extensions.create(NODE_JS, NodeJsExtension::class.java, this)
            }
        }
    }

    private fun setupCleanNodeModulesTask(project: Project) {
        project.tasks.create("cleanNodeModules", Delete::class.java) {
            it.description = "Deletes node_modules and package.json file"
            it.group = BasePlugin.BUILD_GROUP

            it.doLast {
                project.nodeJs.root.packageManager.cleanProject(project)
            }

            project.tasks.maybeCreate(BasePlugin.CLEAN_TASK_NAME).dependsOn(it)
        }

        project.tasks.create("cleanGradleNodeModules", Delete::class.java) {
            it.description = "Deletes node_modules_gradle"
            it.group = BasePlugin.BUILD_GROUP

            it.doLast {
                project.npmProject.gradleNodeModulesDir.deleteRecursively()
            }

            project.tasks.maybeCreate(BasePlugin.CLEAN_TASK_NAME).dependsOn(it)
        }
    }

    companion object {
        fun apply(project: Project): NodeJsExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(NodeJsPlugin::class.java)
            return rootProject.extensions.getByName(NODE_JS) as NodeJsExtension
        }
    }
}
