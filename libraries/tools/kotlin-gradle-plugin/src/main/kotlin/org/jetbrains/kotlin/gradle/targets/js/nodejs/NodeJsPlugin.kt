package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension.Companion.NODE_JS
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolveTask

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
            it.description = "Deletes nodeJs projects created during build"
            it.group = BasePlugin.BUILD_GROUP
            it.delete.add(project.nodeJs.root.rootPackageDir)
        }

        project.tasks.create("cleanGradleNodeModules", Delete::class.java) {
            it.description = "Deletes node modules imported from gradle external modules"
            it.group = BasePlugin.BUILD_GROUP
            it.delete.add(project.nodeJs.root.nodeModulesGradleCacheDir)
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
