/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.RESTORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.UPGRADE_YARN_LOCK
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.detachedResolvable
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File
import kotlin.reflect.KClass

/**
 * A class responsible for applying the Yarn plugin to a Gradle project, specifically targeting
 * root-level projects. This class initializes and configures the necessary extensions,
 * tasks, and environment specifications for managing Yarn as a package manager within Node.js projects.
 * Better to use composition overt than inheritance, that's why it is necessary.
 *
 * @property platformDisambiguate An instance that provides platform-specific disambiguation logic.
 * @property yarnRootKlass The KClass of the root Yarn extension to be created and registered.
 * @property yarnRootName The name used for the root Yarn extension.
 * @property yarnEnvSpecKlass The KClass of the Yarn environment specification extension.
 * @property yarnEnvSpecName The name of the Yarn environment specification extension.
 * @property nodeJsRootApply A lambda function that applies Node.js root extension to the project.
 * @property nodeJsRootExtension A lambda function to retrieve the Node.js root extension from the project.
 * @property nodeJsEnvSpec A lambda function to retrieve Node.js environment specifications from the project.
 * @property lockFileDirectory A function to determine the directory where lock files should be stored.
 */
internal class YarnPluginApplier(
    private val platformDisambiguate: HasPlatformDisambiguate,
    private val yarnRootKlass: KClass<out BaseYarnRootExtension>,
    private val yarnRootName: String,
    private val yarnEnvSpecKlass: KClass<out BaseYarnRootEnvSpec>,
    private val yarnEnvSpecName: String,
    private val nodeJsRootApply: (project: Project) -> Unit,
    private val nodeJsRootExtension: (project: Project) -> BaseNodeJsRootExtension,
    private val nodeJsEnvSpec: (project: Project) -> BaseNodeJsEnvSpec,
    private val lockFileDirectory: (projectDirectory: File) -> File,
) {

    fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "${this::class.java.name} can be applied only to root project"
        }

        nodeJsRootApply(project)

        val nodeJsRoot = nodeJsRootExtension(project)
        val nodeJs = nodeJsEnvSpec(project)

        val yarnSpec = project.extensions.createYarnEnvSpec(
            yarnEnvSpecKlass,
            yarnEnvSpecName
        )

        val yarnRootExtension = project.extensions.create(
            yarnRootName,
            yarnRootKlass.java,
            project,
            nodeJsRoot,
            yarnSpec,
        )

        yarnSpec.initializeYarnEnvSpec(project.objects, yarnRootExtension)

        yarnRootExtension.platform.value(nodeJs.platform)
            .disallowChanges()

        nodeJsRoot.packageManagerExtension.set(
            yarnRootExtension
        )

        val setupTask = project.registerTask<YarnSetupTask>(platformDisambiguate.extensionName(YarnSetupTask.NAME), listOf(yarnSpec)) {
            with(nodeJs) {
                it.dependsOn(project.nodeJsSetupTaskProvider)
            }

            it.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Download and install a local yarn version"

            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedResolvable(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        val kotlinNpmInstall = project.tasks.named(platformDisambiguate.extensionName(KotlinNpmInstallTask.NAME))
        kotlinNpmInstall.configure {
            it.dependsOn(setupTask)
            it.inputs.property("yarnIgnoreScripts", { yarnRootExtension.ignoreScripts })
        }

        yarnRootExtension.nodeJsEnvironment.value(
            nodeJs.env
        ).disallowChanges()

        project.tasks.register(platformDisambiguate.extensionName("yarn" + CleanDataTask.NAME_SUFFIX), CleanDataTask::class.java) {
            it.cleanableStoreProvider = project.provider { yarnRootExtension.requireConfigured().cleanableStore }
            it.description = "Clean unused local yarn version"
        }

        yarnRootExtension.lockFileDirectory = lockFileDirectory(project.rootDir)

        project.tasks.register(platformDisambiguate.extensionName(STORE_YARN_LOCK_NAME), YarnLockStoreTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) })
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)

            task.lockFileMismatchReport.value(
                project.provider { yarnRootExtension.requireConfigured().yarnLockMismatchReport.toLockFileMismatchReport() }
            ).disallowChanges()
            task.reportNewLockFile.value(
                project.provider { yarnRootExtension.requireConfigured().reportNewYarnLock }
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                project.provider { yarnRootExtension.requireConfigured().yarnLockAutoReplace }
            ).disallowChanges()
        }

        project.tasks.register(platformDisambiguate.extensionName(UPGRADE_YARN_LOCK), YarnLockCopyTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) })
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)
        }

        project.tasks.register(platformDisambiguate.extensionName(RESTORE_YARN_LOCK_NAME), YarnLockCopyTask::class.java) {
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

    private fun ExtensionContainer.createYarnEnvSpec(
        yarnEnvSpecKlass: KClass<out BaseYarnRootEnvSpec>,
        yarnEnvSpecName: String,
    ): BaseYarnRootEnvSpec {
        return create(
            yarnEnvSpecName,
            yarnEnvSpecKlass.java
        )
    }

    private fun BaseYarnRootEnvSpec.initializeYarnEnvSpec(
        objectFactory: ObjectFactory,
        yarnRootExtension: BaseYarnRootExtension,
    ) {
        download.convention(yarnRootExtension.downloadProperty)
        downloadBaseUrl.set(yarnRootExtension.downloadBaseUrlProperty)
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
}