/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask.Companion.DEPRECATION_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import kotlin.reflect.KClass

internal class NodeJsRootPluginApplier(
    private val platformDisambiguate: HasPlatformDisambiguator,
    private val nodeJsRootKlass: KClass<out BaseNodeJsRootExtension>,
    private val nodeJsRootName: String,
    private val npmKlass: KClass<out BaseNpmExtension>,
    private val npmName: String,
    private val rootDirectoryName: String,
    private val lockFileDirectory: (projectDirectory: Directory) -> Directory,
    private val singleNodeJsPluginApply: (project: Project) -> BaseNodeJsEnvSpec,
    private val yarnPlugin: KClass<out Plugin<Project>>,
    private val platformType: KotlinPlatformType,
) {

    fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "${this::class.java.name} can be applied only to root project"
        }

        project.plugins.apply(BasePlugin::class.java)

        val nodeJsRoot = project.extensions.create(
            nodeJsRootName,
            nodeJsRootKlass.java,
            project,
            { singleNodeJsPluginApply(project) },
            rootDirectoryName,
        )

        val npm = project.extensions.create(
            npmName,
            npmKlass.java,
            project,
            nodeJsRoot,
        )

        val nodeJs = singleNodeJsPluginApply(project)

        npm.nodeJsEnvironment.value(
            nodeJs.env
        ).disallowChanges()

        nodeJsRoot.packageManagerExtension.convention(
            npm
        )

        npm.lockFileDirectory.convention(
            lockFileDirectory(project.layout.projectDirectory)
        )

        val gradleNodeModulesProvider: Provider<GradleNodeModulesCache> = GradleNodeModulesCache.registerIfAbsent(
            project,
            project.projectDir,
            nodeJsRoot.nodeModulesGradleCacheDirectory,
            platformDisambiguate::extensionName
        )

        val setupFileHasherTask =
            project.registerTask<KotlinNpmCachesSetup>(platformDisambiguate.extensionName(KotlinNpmCachesSetup.NAME)) {
                it.description = "Setup file hasher for caches"

                it.gradleNodeModules.set(gradleNodeModulesProvider)
            }

        project.registerTask<Task>(platformDisambiguate.extensionName(PACKAGE_JSON_UMBRELLA_TASK_NAME))

        nodeJsRoot.resolver = KotlinRootNpmResolver(
            project.name,
            project.version.toString(),
            TasksRequirements(),
            nodeJsRoot.versions,
            nodeJsRoot.rootProjectDir,
            platformType
        )

        val objectFactory = project.objects

        val npmResolutionManager: Provider<KotlinNpmResolutionManager> = KotlinNpmResolutionManager.registerIfAbsent(
            project,
            objectFactory.providerWithLazyConvention {
                nodeJsRoot.resolver.close()
            },
            gradleNodeModulesProvider,
            nodeJsRoot.projectPackagesDirectory
        ) {
            platformDisambiguate.extensionName(it, prefix = null)
        }

        val packageJsonUmbrella = nodeJsRoot
            .packageJsonUmbrellaTaskProvider

        val rootPackageJson =
            project.tasks.register(
                platformDisambiguate.extensionName(
                    RootPackageJsonTask.NAME,
                    prefix = null,
                ),
                RootPackageJsonTask::class.java
            ) { task ->
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME
                task.description = "Create root package.json"

                task.configureNodeJsEnvironmentWithNpmResolutionManagerTasks(
                    setupFileHasherTask,
                    nodeJsRoot,
                    nodeJs,
                    npmResolutionManager,
                )

                task.rootPackageJsonFile.value(
                    nodeJsRoot.rootPackageDirectory.map { it.file(NpmProject.PACKAGE_JSON) }
                ).disallowChanges()

                task.onlyIf("Prepare NPM project only in configuring state") {
                    it as RootPackageJsonTask
                    it.npmResolutionManager.get().isConfiguringState()
                }

                task.dependsOn(packageJsonUmbrella)
            }

        configureRequiresNpmDependencies(project, rootPackageJson)

        val npmInstall =
            project.registerTask<KotlinNpmInstallTask>(platformDisambiguate.extensionName(KotlinNpmInstallTask.BASE_NAME)) { npmInstall ->
                with(nodeJs) {
                    npmInstall.dependsOn(project.nodeJsSetupTaskProvider)
                }
                npmInstall.group = NodeJsRootPlugin.TASKS_GROUP_NAME
                npmInstall.description = "Find, download and link NPM dependencies and projects"

                npmInstall.configureNodeJsEnvironmentWithNpmResolutionManagerTasks(
                    setupFileHasherTask,
                    nodeJsRoot,
                    nodeJs,
                    npmResolutionManager,
                )

                npmInstall.nodeModules.value(
                    nodeJsRoot.rootPackageDirectory.map { it.dir("node_modules") }
                ).disallowChanges()

                npmInstall.additionalFiles.from(
                    nodeJsRoot.packageManagerExtension.map { it.additionalInstallOutput }
                ).disallowChanges()

                npmInstall.preparedFiles.from(
                    nodeJsRoot.packageManagerExtension.zip(npmInstall.nodeJsEnvironment) { npmApiExt, nodeJsEnvironment ->
                        npmApiExt.packageManager.preparedFiles(nodeJsEnvironment)
                    }
                ).disallowChanges()

                npmInstall.onlyIf("No package.json files for install") { task ->
                    task as KotlinNpmInstallTask
                    task.preparedFiles.all { file ->
                        file.exists()
                    }
                }

                npmInstall.outputs.upToDateWhen {
                    npmInstall.nodeModules.getFile().exists()
                }

                npmInstall.dependsOn(rootPackageJson)
                npmInstall.inputs.property("npmIgnoreScripts", { npm.ignoreScripts })

                npmInstall.dependsOn(nodeJsRoot.packageManagerExtension.map { it.preInstallTasks })
            }

        val upgradeLock = project.tasks.register(
            platformDisambiguate.extensionName(LockCopyTask.UPGRADE_PACKAGE_LOCK_BASE_NAME),
            LockStoreTask::class.java
        ) { task ->
            task.dependsOn(npmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.PACKAGE_LOCK) })
            task.outputDirectory.set(npm.lockFileDirectory)
            task.fileName.set(npm.lockFileName)

            task.additionalInputFiles.from(
                nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )
            task.additionalInputFiles.from(
                task.outputDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )

            task.lockFileMismatchReport.value(
                LockFileMismatchReport.NONE
            ).disallowChanges()
            task.reportNewLockFile.value(
                false
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                true
            ).disallowChanges()
        }

        project.tasks.register(
            platformDisambiguate.extensionName(LockCopyTask.STORE_PACKAGE_LOCK_BASE_NAME),
            LockStoreTask::class.java
        ) { task ->
            task.dependsOn(npmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.PACKAGE_LOCK) })

            task.additionalInputFiles.from(
                nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )
            task.additionalInputFiles.from(
                task.outputDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )

            task.outputDirectory.set(npm.lockFileDirectory)
            task.fileName.set(npm.lockFileName)

            task.lockFileMismatchReport.value(
                project.provider { npm.requireConfigured().packageLockMismatchReport }
            ).disallowChanges()
            task.reportNewLockFile.value(
                project.provider { npm.requireConfigured().reportNewPackageLock }
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                project.provider { npm.requireConfigured().packageLockAutoReplace }
            ).disallowChanges()
            task.mismatchMessage.value(
                LockCopyTask.packageLockMismatchMessage(
                    upgradeLock.name
                )
            )
        }

        project.tasks.register(
            platformDisambiguate.extensionName(LockCopyTask.RESTORE_PACKAGE_LOCK_BASE_NAME),
            LockCopyTask::class.java
        ) { task ->
            task.inputFile.set(
                npm.lockFileDirectory.flatMap { dir ->
                    dir.file(npm.lockFileName)
                }
            )
            task.additionalInputFiles.from(
                npm.lockFileDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )
            task.outputDirectory.set(nodeJsRoot.rootPackageDirectory)
            task.fileName.set(LockCopyTask.PACKAGE_LOCK)
            task.onlyIf {
                val inputFileExists = task.inputFile.getOrNull()?.asFile?.exists() == true
                // Workaround for "skip if not exists"
                // https://github.com/gradle/gradle/issues/2919
                if (!inputFileExists) {
                    task.inputFile.set(null as RegularFile?)
                }
                inputFileExists || task.additionalInputFiles.files.any { it.exists() }
            }
        }

        npm.preInstallTasks.value(
            listOf(npm.restorePackageLockTaskProvider)
        ).disallowChanges()

        npm.postInstallTasks.value(
            listOf(npm.storePackageLockTaskProvider)
        ).disallowChanges()

        project.tasks.register(
            platformDisambiguate.extensionName(
                "node" + CleanDataTask.NAME_SUFFIX,
                prefix = null,
            ),
            CleanDataTask::class.java
        ) {
            it.doFirst {
                it.logger.warn(DEPRECATION_MESSAGE)
            }

            it.cleanableStoreProvider = nodeJs
                .installationDirectory
                .map { CleanableStore.Companion[it.asFile.path] }
            it.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Clean unused local node version"
        }

        val propertiesProvider = PropertiesProvider.Companion(project)

        if (propertiesProvider.yarn) {
            project.plugins.apply(yarnPlugin.java)
        }
    }

    private fun PackageJsonFilesTask.configureNodeJsEnvironmentWithNpmResolutionManagerTasks(
        setupFileHasherTask: TaskProvider<*>,
        nodeJsRoot: BaseNodeJsRootExtension,
        nodeJs: BaseNodeJsEnvSpec,
        npmResolutionManager: Provider<KotlinNpmResolutionManager>,
    ) {
        dependsOn(setupFileHasherTask)

        this.npmResolutionManager.value(npmResolutionManager)
            .disallowChanges()

        packageJsonFiles.value(
            nodeJsRoot.projectPackagesDirectory.map { projectPackagesDirectory ->
                nodeJsRoot.resolver
                    .projectResolvers.values
                    .flatMap { it.compilationResolvers }
                    .map { it.compilationNpmResolution }
                    .map { resolution ->
                        val name = resolution.npmProjectName
                        projectPackagesDirectory.dir(name).file(NpmProject.PACKAGE_JSON)
                    }
            }
        ).disallowChanges()

        configureNodeJsEnvironmentTasks(
            nodeJsRoot,
            nodeJs
        )
    }

    // Yes, we need to break Task Configuration Avoidance here
    // In case when we need to create package.json's files and execute kotlinNpmInstall,
    // We need to configure all RequiresNpmDependencies tasks to install them,
    // Because we need to persist lock file
    // We execute this block in configure phase of rootPackageJson to be sure,
    // That Task Configuration Avoidance will not be broken for tasks not related with NPM installing
    // https://youtrack.jetbrains.com/issue/KT-48241
    private fun configureRequiresNpmDependencies(
        project: Project,
        rootPackageJson: TaskProvider<RootPackageJsonTask>,
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
}

internal fun NodeJsEnvironmentTask.configureNodeJsEnvironmentTasks(
    nodeJsRoot: BaseNodeJsRootExtension,
    nodeJs: BaseNodeJsEnvSpec,
) {
    val rootPackageDirectory = nodeJsRoot.rootPackageDirectory
    val packageManager = nodeJsRoot.packageManagerExtension.map { it.packageManager }

    nodeJsEnvironment
        .value(
            nodeJs.env.map {
                asNodeJsEnvironment(rootPackageDirectory, packageManager, it)
            }
        )
        .disallowChanges()

    packageManagerEnv.value(
        nodeJsRoot.packageManagerExtension.map { it.environment }
    ).disallowChanges()
}