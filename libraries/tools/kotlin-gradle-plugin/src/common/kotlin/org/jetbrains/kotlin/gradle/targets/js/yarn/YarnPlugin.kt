/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.detachedResolvable
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention

open class YarnPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "YarnPlugin can be applied only to root project"
        }

        NodeJsRootPlugin.apply(project)
        val nodeJsRoot = this.kotlinNodeJsRootExtension
        val nodeJs = this.kotlinNodeJsEnvSpec

        val yarnSpec = project.extensions.createYarnEnvSpec()

        val yarnRootExtension = this.extensions.create(
            YarnRootExtension.YARN,
            YarnRootExtension::class.java,
            this,
            nodeJsRoot,
            yarnSpec
        )

        yarnSpec.initializeYarnEnvSpec(objects, yarnRootExtension)

        yarnRootExtension.platform.value(nodeJs.platform)
            .disallowChanges()

        nodeJsRoot.packageManagerExtension.set(
            yarnRootExtension
        )

        val setupTask = registerTask<YarnSetupTask>(YarnSetupTask.NAME, listOf(yarnSpec)) {
            with(nodeJs) {
                it.dependsOn(project.nodeJsSetupTaskProvider)
            }

            it.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Download and install a local yarn version"

            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                this.project.configurations.detachedResolvable(this.project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        val kotlinNpmInstall = tasks.named(KotlinNpmInstallTask.NAME)
        kotlinNpmInstall.configure {
            it.dependsOn(setupTask)
            it.inputs.property("yarnIgnoreScripts", { yarnRootExtension.ignoreScripts })
        }

        yarnRootExtension.nodeJsEnvironment.value(
            nodeJs.env
        ).disallowChanges()

        tasks.register("yarn" + CleanDataTask.NAME_SUFFIX, CleanDataTask::class.java) {
            it.cleanableStoreProvider = provider { yarnRootExtension.requireConfigured().cleanableStore }
            it.description = "Clean unused local yarn version"
        }

        tasks.register(STORE_YARN_LOCK_NAME, YarnLockStoreTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) })
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)

            task.lockFileMismatchReport.value(
                provider { yarnRootExtension.requireConfigured().yarnLockMismatchReport.toLockFileMismatchReport() }
            ).disallowChanges()
            task.reportNewLockFile.value(
                provider { yarnRootExtension.requireConfigured().reportNewYarnLock }
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                provider { yarnRootExtension.requireConfigured().yarnLockAutoReplace }
            ).disallowChanges()
        }

        tasks.register(UPGRADE_YARN_LOCK, YarnLockCopyTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) })
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)
        }

        tasks.register(RESTORE_YARN_LOCK_NAME, YarnLockCopyTask::class.java) {
            val lockFile = yarnRootExtension.lockFileDirectory.resolve(yarnRootExtension.lockFileName)
            it.inputFile.set(yarnRootExtension.lockFileDirectory.resolve(yarnRootExtension.lockFileName))
            it.outputDirectory.set(nodeJsRoot.rootPackageDirectory)
            it.fileName.set(LockCopyTask.YARN_LOCK)
            it.onlyIf {
                lockFile.exists()
            }
        }

        yarnRootExtension.preInstallTasks.value(
            listOf(yarnRootExtension.restoreYarnLockTaskProvider)
        ).disallowChanges()

        yarnRootExtension.postInstallTasks.value(
            listOf(yarnRootExtension.storeYarnLockTaskProvider)
        ).disallowChanges()
    }

    private fun ExtensionContainer.createYarnEnvSpec(): YarnRootEnvSpec {
        return create(
            YarnRootEnvSpec.YARN,
            YarnRootEnvSpec::class.java
        )
    }

    private fun YarnRootEnvSpec.initializeYarnEnvSpec(
        objectFactory: ObjectFactory,
        yarnRootExtension: YarnRootExtension,
    ) {
        download.convention(yarnRootExtension.downloadProperty)
        downloadBaseUrl.convention(yarnRootExtension.downloadBaseUrlProperty)
        allowInsecureProtocol.convention(false)
        installationDirectory.convention(yarnRootExtension.installationDirectory)
        version.convention(yarnRootExtension.versionProperty)
        command.convention(yarnRootExtension.commandProperty)
        platform.convention(yarnRootExtension.platform)
        ignoreScripts.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.ignoreScripts })
        yarnLockMismatchReport.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.yarnLockMismatchReport })
        reportNewYarnLock.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.reportNewYarnLock })
        yarnLockAutoReplace.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.yarnLockAutoReplace })
        resolutions.convention(
            objectFactory.listProperty<YarnResolution>().value(
                objectFactory.providerWithLazyConvention {
                    yarnRootExtension.resolutions
                }
            )
        )
    }

    companion object {
        fun apply(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YarnRootExtension.YARN) as YarnRootExtension
        }

        const val STORE_YARN_LOCK_NAME = "kotlinStoreYarnLock"
        const val RESTORE_YARN_LOCK_NAME = "kotlinRestoreYarnLock"
        const val UPGRADE_YARN_LOCK = "kotlinUpgradeYarnLock"
        const val YARN_LOCK_MISMATCH_MESSAGE = "Lock file was changed. Run the `${UPGRADE_YARN_LOCK}` task to actualize lock file"
    }
}
