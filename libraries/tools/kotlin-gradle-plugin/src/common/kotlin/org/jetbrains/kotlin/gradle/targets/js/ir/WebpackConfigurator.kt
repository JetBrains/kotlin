/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinBrowserJsIr.Companion.WEBPACK_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.DISTRIBUTION_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

class WebpackConfigurator(private val subTarget: KotlinJsIrSubTarget) : SubTargetConfigurator<KotlinWebpack, KotlinWebpack> {

    private val project = subTarget.project

    private val nodeJsRoot = subTarget.target.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    private val webpackTaskConfigurations = project.objects.domainObjectSet<Action<KotlinWebpack>>()
    private val runTaskConfigurations = project.objects.domainObjectSet<Action<KotlinWebpack>>()

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        val target = compilation.target
        val project = target.project

        val processResourcesTask = project.tasks.named(compilation.processResourcesTaskName)

        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

        compilation.binaries
            .withType<Executable>()
            .configureEach { binary ->
                binary as Executable

                val mode = binary.mode
                val archivesName = project.archivesName

                val linkSyncTask = binary.linkSyncTask

                val webpackTask = subTarget.registerSubTargetTask<KotlinWebpack>(
                    subTarget.disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    task.description = "build webpack ${mode.name.toLowerCaseAsciiOnly()} bundle"
                    val buildDirectory = project.layout.buildDirectory
                    val targetName = target.name
                    task.outputDirectory.convention(
                        binary.distribution.distributionName.flatMap {
                            buildDirectory.dir("kotlin-webpack/$targetName/$it")
                        }
                    ).finalizeValueOnRead()

                    task.dependsOn(linkSyncTask)

                    task.commonConfigure(
                        binary = binary,
                        mode = mode,
                        // properties could be read by other agents (for example Android Studio)
                        // without a wrapper,
                        // it could force the error about querying a value of a task which is not executed yet
                        inputFilesDirectory = task.project.objects.directoryProperty().fileProvider(
                            task.project.provider { linkSyncTask.get().destinationDirectory.get() },
                        ).also { it.finalizeValueOnRead() },
                        entryModuleName = binary.linkTask.flatMap { it.compilerOptions.moduleName },
                        configurationActions = webpackTaskConfigurations,
                        defaultArchivesName = archivesName,
                    )
                }

                val distributionTask = subTarget.registerSubTargetTask<Sync>(
                    subTarget.disambiguateCamelCased(
                        if (binary.mode == KotlinJsBinaryMode.PRODUCTION && binary.compilation.isMain())
                            ""
                        else
                            binary.name,
                        DISTRIBUTION_TASK_NAME
                    )
                ) { copy ->
                    copy.from(processResourcesTask)
                    copy.from(webpackTask.flatMap { it.outputDirectory })

                    copy.into(binary.distribution.outputDirectory)
                }

                if (mode == KotlinJsBinaryMode.PRODUCTION && binary.compilation.isMain()) {
                    assembleTaskProvider.dependsOn(distributionTask)
                }
            }
    }

    override fun configureBuild(body: Action<KotlinWebpack>) {
        webpackTaskConfigurations.add(body)
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
        val target = compilation.target
        val project = target.project

        compilation.binaries
            .withType<Executable>()
            .configureEach { binary ->
                binary as Executable

                val mode = binary.mode
                val archivesName = project.archivesName

                val linkSyncTask = binary.linkSyncTask

                subTarget.registerSubTargetTask<KotlinWebpack>(
                    subTarget.disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    task.dependsOn(linkSyncTask)

                    task.args.add(0, "serve")
                    task.description = "start ${mode.name.toLowerCaseAsciiOnly()} webpack dev server"

                    val npmProject = compilation.npmProject
                    val resourcesDir = compilation.output.resourcesDir
                    task.devServerProperty.convention(
                        npmProject.dist.zip(npmProject.dir) { distDirectory, dir ->
                            KotlinWebpackConfig.DevServer(
                                open = true,
                                static = mutableListOf(
                                    distDirectory.asFile.normalize().relativeOrAbsolute(dir.asFile),
                                    resourcesDir.relativeOrAbsolute(dir.asFile),
                                ),
                                client = KotlinWebpackConfig.DevServer.Client(
                                    KotlinWebpackConfig.DevServer.Client.Overlay(
                                        errors = true,
                                        warnings = false
                                    )
                                )
                            )
                        }
                    )

                    task.watchOptions = KotlinWebpackConfig.WatchOptions(
                        ignored = arrayOf("*.kt")
                    )


                    task.doNotTrackState("Tracked by external webpack tool")

                    task.dependsOn(linkSyncTask)

                    task.commonConfigure(
                        binary = binary,
                        mode = mode,
                        // properties could be read by other agents (for example Android Studio)
                        // without a wrapper,
                        // it could force the error about querying a value of a task which is not executed yet
                        inputFilesDirectory = task.project.objects.directoryProperty().fileProvider(
                            task.project.provider { linkSyncTask.get().destinationDirectory.get() },
                        ).also { it.finalizeValueOnRead() },
                        entryModuleName = binary.linkTask.flatMap { it.compilerOptions.moduleName },
                        configurationActions = runTaskConfigurations,
                        defaultArchivesName = archivesName,
                    )
                }
            }
    }

    override fun configureRun(body: Action<KotlinWebpack>) {
        runTaskConfigurations.add(body)
    }

    private fun KotlinWebpack.commonConfigure(
        binary: JsIrBinary,
        mode: KotlinJsBinaryMode,
        inputFilesDirectory: Provider<Directory>,
        entryModuleName: Provider<String>,
        configurationActions: DomainObjectSet<Action<KotlinWebpack>>,
        defaultArchivesName: Property<String>,
    ) {
        val target = binary.target

        dependsOn(
            target.project.tasks.named(compilation.processResourcesTaskName)
        )

        dependsOn(nodeJsRoot.npmInstallTaskProvider)

        dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })

        configureOptimization(mode)

        this.versions.value(nodeJsRoot.versions)
            .disallowChanges()
        this.rootPackageDir.value(nodeJsRoot.rootPackageDirectory)
            .disallowChanges()

        this.inputFilesDirectory.set(inputFilesDirectory)

        val platformType = binary.compilation.platformType
        val moduleKind = binary.linkTask.flatMap { task ->
            task.compilerOptions.moduleKind.orElse(task.compilerOptions.target.map {
                if (it == ES_2015) JsModuleKind.MODULE_ES else JsModuleKind.MODULE_UMD
            })
        }

        this.entryModuleName.set(entryModuleName)
        this.esModules.convention(
            project.provider {
                platformType == KotlinPlatformType.wasm || moduleKind.get() == JsModuleKind.MODULE_ES
            }
        ).finalizeValueOnRead()

        mainOutputFileName.convention(defaultArchivesName.orElse("main").map { "$it.js" }).finalizeValueOnRead()

        configurationActions.all { configure ->
            configure.execute(this)
        }
    }

    private fun KotlinWebpack.configureOptimization(mode: KotlinJsBinaryMode) {
        this.mode = getByKind(
            kind = mode,
            releaseValue = Mode.PRODUCTION,
            debugValue = Mode.DEVELOPMENT
        )

        devtool = getByKind(
            kind = mode,
            releaseValue = WebpackDevtool.SOURCE_MAP,
            debugValue = WebpackDevtool.EVAL_SOURCE_MAP
        )
    }

    private fun <T> getByKind(
        kind: KotlinJsBinaryMode,
        releaseValue: T,
        debugValue: T,
    ): T = when (kind) {
        KotlinJsBinaryMode.PRODUCTION -> releaseValue
        KotlinJsBinaryMode.DEVELOPMENT -> debugValue
    }
}
