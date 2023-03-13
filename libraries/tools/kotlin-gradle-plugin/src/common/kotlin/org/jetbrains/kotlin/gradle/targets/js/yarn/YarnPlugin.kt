/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.utils.markResolvable
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.RESTORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.UPGRADE_YARN_LOCK
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.onlyIfCompat

open class YarnPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "YarnPlugin can be applied only to root project"
        }

        val yarnRootExtension = this.extensions.create(YarnRootExtension.YARN, YarnRootExtension::class.java, this)
        NodeJsRootPlugin.apply(project)
        val nodeJs = this.kotlinNodeJsExtension
        val nodeJsTaskProviders = this.kotlinNodeJsExtension

        val setupTask = registerTask<YarnSetupTask>(YarnSetupTask.NAME) {
            it.dependsOn(nodeJsTaskProviders.nodeJsSetupTaskProvider)

            it.configuration = provider {
                this.project.configurations.detachedConfiguration(this.project.dependencies.create(it.ivyDependency))
                    .markResolvable()
                    .also { conf -> conf.isTransitive = false }
            }
        }

        val kotlinNpmResolutionManager = project.kotlinNpmResolutionManager

        val rootPackageJson = tasks.register(RootPackageJsonTask.NAME, RootPackageJsonTask::class.java) { task ->
            task.dependsOn(nodeJsTaskProviders.npmCachesSetupTaskProvider)
            task.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            task.description = "Create root package.json"

            task.npmResolutionManager.value(kotlinNpmResolutionManager)
                .disallowChanges()

            task.onlyIfCompat("Prepare NPM project only in configuring state") {
                it as RootPackageJsonTask
                it.npmResolutionManager.get().isConfiguringState()
            }
        }

        configureRequiresNpmDependencies(project, rootPackageJson)

        val kotlinNpmInstall = tasks.named(KotlinNpmInstallTask.NAME)
        kotlinNpmInstall.configure {
            it.dependsOn(rootPackageJson)
            it.dependsOn(setupTask)
            it.inputs.property("ignoreScripts", { yarnRootExtension.ignoreScripts })
        }

        tasks.register("yarn" + CleanDataTask.NAME_SUFFIX, CleanDataTask::class.java) {
            it.cleanableStoreProvider = provider { yarnRootExtension.requireConfigured().cleanableStore }
            it.description = "Clean unused local yarn version"
        }

        val packageJsonUmbrella = nodeJsTaskProviders
            .packageJsonUmbrellaTaskProvider

        yarnRootExtension.rootPackageJsonTaskProvider.configure {
            it.dependsOn(packageJsonUmbrella)
        }

        tasks.register(STORE_YARN_LOCK_NAME, YarnLockStoreTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJs.rootPackageDir.resolve("yarn.lock"))
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)

            task.yarnLockMismatchReport = provider { yarnRootExtension.requireConfigured().yarnLockMismatchReport }
            task.reportNewYarnLock = provider { yarnRootExtension.requireConfigured().reportNewYarnLock }
            task.yarnLockAutoReplace = provider { yarnRootExtension.requireConfigured().yarnLockAutoReplace }
        }

        tasks.register(UPGRADE_YARN_LOCK, YarnLockCopyTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJs.rootPackageDir.resolve("yarn.lock"))
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)
        }

        val restoreYarnLock = tasks.register(RESTORE_YARN_LOCK_NAME, YarnLockCopyTask::class.java) {
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
        }
    }

    // Yes, we need to break Task Configuration Avoidance here
    // In case when we need to create package.json's files and execute kotlinNpmInstall,
    // We need to configure all RequiresNpmDependencies tasks to install them,
    // Because we need to persist yarn.lock
    // We execute this block in configure phase of rootPackageJson to be sure,
    // That Task Configuration Avoidance will not be broken for tasks not related with NPM installing
    // https://youtrack.jetbrains.com/issue/KT-48241
    private fun configureRequiresNpmDependencies(
        project: Project,
        rootPackageJson: TaskProvider<RootPackageJsonTask>
    ) {
        val fn: (Project) -> Unit = {
            it.tasks.implementing(RequiresNpmDependencies::class)
                .forEach {}
        }
        rootPackageJson.configure {
            project.allprojects
                .forEach { project ->
                    if (it.state.executed) {
                        fn(project)
                    }
                }
        }

        project.allprojects
            .forEach {
                if (!it.state.executed) {
                    it.afterEvaluate { project ->
                        rootPackageJson.configure {
                            fn(project)
                        }
                    }
                }
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
