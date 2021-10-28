/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
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

        val setupTask = registerTask<YarnSetupTask>(YarnSetupTask.NAME) {
            it.dependsOn(nodeJs.nodeJsSetupTaskProvider)

            it.configuration = provider {
                this.project.configurations.detachedConfiguration(this.project.dependencies.create(it.ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        val rootClean = project.rootProject.tasks.named(BasePlugin.CLEAN_TASK_NAME)

        val rootPackageJson = tasks.register(RootPackageJsonTask.NAME, RootPackageJsonTask::class.java) { task ->
            task.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            task.description = "Create root package.json"

            task.mustRunAfter(rootClean)

            // Yes, we need to break Task Configuration Avoidance here
            // In case when we need to create package.json's files and execute kotlinNpmInstall,
            // We need to configure all RequiresNpmDependencies tasks to install them,
            // Because we need to persist yarn.lock
            // We execute this block in configure phase of rootPackageJson to be sure,
            // That Task Configuration Avoidance will not be broken for tasks not related with NPM installing
            // https://youtrack.jetbrains.com/issue/KT-48241
            project.allprojects
                .forEach {
                    it.tasks.implementing(RequiresNpmDependencies::class)
                        .forEach {}
                }
        }

        val kotlinNpmInstall = tasks.named(KotlinNpmInstallTask.NAME)
        kotlinNpmInstall.configure {
            it.dependsOn(rootPackageJson)
            it.dependsOn(setupTask)
        }

        tasks.register("yarn" + CleanDataTask.NAME_SUFFIX, CleanDataTask::class.java) {
            it.cleanableStoreProvider = provider { yarnRootExtension.requireConfigured().cleanableStore }
            it.description = "Clean unused local yarn version"
        }

        val packageJsonUmbrella = nodeJs
            .packageJsonUmbrellaTaskProvider

        yarnRootExtension.rootPackageJsonTaskProvider.configure {
            it.dependsOn(packageJsonUmbrella)
        }

//        project.allprojects
//            .forEach {
//                val fn: (Project) -> Unit = {
//                    it.tasks.implementing(RequiresNpmDependencies::class).all {}
//                }
//                if (it.state.executed) {
//                    fn(it)
//                } else {
//                    it.afterEvaluate {
//                        fn(it)
//                    }
//                }
//            }

        val storeYarnLock = tasks.register("kotlinStoreYarnLock", YarnLockCopyTask::class.java) {
            it.dependsOn(kotlinNpmInstall)
            it.inputFile.set(nodeJs.rootPackageDir.resolve("yarn.lock"))
            it.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            it.fileName.set(yarnRootExtension.lockFileName)
        }

        val restoreYarnLock = tasks.register("kotlinRestoreYarnLock", YarnLockCopyTask::class.java) {
            val lockFile = yarnRootExtension.lockFileDirectory.resolve(yarnRootExtension.lockFileName)
            it.inputFile.set(yarnRootExtension.lockFileDirectory.resolve(yarnRootExtension.lockFileName))
            it.outputDirectory.set(nodeJs.rootPackageDir)
            it.fileName.set("yarn.lock")
            it.onlyIf {
                lockFile.exists()
            }
        }

        kotlinNpmInstall.configure {
            it.dependsOn(restoreYarnLock)
            it.finalizedBy(storeYarnLock)
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
