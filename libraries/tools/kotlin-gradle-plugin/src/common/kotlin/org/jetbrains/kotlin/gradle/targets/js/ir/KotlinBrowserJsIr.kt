/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDceDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.doNotTrackStateCompat
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File
import javax.inject.Inject

abstract class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val nodeJs = project.rootProject.kotlinNodeJsExtension

    private val webpackTaskConfigurations: DomainObjectSet<Action<KotlinWebpack>> = project.objects.domainObjectSet(Action::class.java)
            as DomainObjectSet<Action<KotlinWebpack>>
    private val runTaskConfigurations: DomainObjectSet<Action<KotlinWebpack>> = project.objects.domainObjectSet(Action::class.java)
            as DomainObjectSet<Action<KotlinWebpack>>

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureTestDependencies(test: KotlinJsTest) {
        test.dependsOn(
            nodeJs.npmInstallTaskProvider,
            nodeJs.storeYarnLockTaskProvider,
            nodeJs.nodeJsSetupTaskProvider
        )
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        if (test.testFramework == null) {
            test.useKarma {
                if (compilation.platformType == KotlinPlatformType.wasm) {
                    useChromeHeadlessWasmGc()
                } else {
                    useChromeHeadless()
                }
            }
        }

        if (test.enabled) {
            nodeJs.taskRequirements.addTaskRequirements(test)
        }
    }

    override fun commonWebpackConfig(body: Action<KotlinWebpackConfig>) {
        webpackTaskConfigurations.add {
            it.webpackConfigApplier(body)
        }
        runTaskConfigurations.add {
            it.webpackConfigApplier(body)
        }
        testTask(Action {
            it.onTestFrameworkSet {
                if (it is KotlinKarma) {
                    body.execute(it.webpackConfig)
                }
            }
        })
    }

    override fun runTask(body: Action<KotlinWebpack>) {
        runTaskConfigurations.add(body)
    }

    override fun webpackTask(body: Action<KotlinWebpack>) {
        webpackTaskConfigurations.add(body)
    }

    @ExperimentalDceDsl
    override fun dceTask(body: Action<KotlinJsDce>) {
        project.logger.warn("dceTask configuration is useless with IR compiler. Use @JsExport on declarations instead.")
    }

    override fun configureRun(
        compilation: KotlinJsIrCompilation
    ) {
        val commonRunTask = registerSubTargetTask<Task>(disambiguateCamelCased(RUN_TASK_NAME)) {}

        compilation.binaries
            .matching { it is Executable }
            .all { binary ->
                binary as Executable

                val mode = binary.mode
                val archivesName = project.archivesName

                val runTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    task.dependsOn(binary.linkSyncTask)

                    task.args.add(0, "serve")
                    task.description = "start ${mode.name.toLowerCaseAsciiOnly()} webpack dev server"

                    val npmProject = compilation.npmProject
                    task.devServer = KotlinWebpackConfig.DevServer(
                        open = true,
                        static = mutableListOf(
                            npmProject.dist.normalize().relativeOrAbsolute(npmProject.dir),
                            compilation.output.resourcesDir.relativeOrAbsolute(npmProject.dir),
                        ),
                        client = KotlinWebpackConfig.DevServer.Client(
                            KotlinWebpackConfig.DevServer.Client.Overlay(
                                errors = true,
                                warnings = false
                            )
                        )
                    )

                    task.watchOptions = KotlinWebpackConfig.WatchOptions(
                        ignored = arrayOf("*.kt")
                    )


                    task.doNotTrackStateCompat("Tracked by external webpack tool")

                    task.dependsOn(binary.linkSyncTask)

                    task.commonConfigure(
                        binary = binary,
                        mode = mode,
                        inputFilesDirectory = task.project.provider { binary.linkSyncTask.get().destinationDirectory.get() },
                        entryModuleName = binary.linkTask.flatMap { it.compilerOptions.moduleName },
                        configurationActions = runTaskConfigurations,
                        nodeJs = nodeJs,
                        defaultArchivesName = archivesName,
                    )
                }

                if (mode == KotlinJsBinaryMode.DEVELOPMENT) {
                    target.runTask.dependsOn(runTask)
                    commonRunTask.dependsOn(runTask)
                }
            }
    }

    override fun configureBuild(
        compilation: KotlinJsIrCompilation
    ) {
        val project = compilation.target.project

        val processResourcesTask = target.project.tasks.named(compilation.processResourcesTaskName)

        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

        compilation.binaries
            .matching { it is Executable }
            .all { binary ->
                binary as Executable

                val mode = binary.mode
                val archivesName = project.archivesName

                val distributeResourcesTask = registerSubTargetTask<Copy>(
                    disambiguateCamelCased(
                        binary.name,
                        DISTRIBUTE_RESOURCES_TASK_NAME
                    )
                ) { copy ->
                    copy.from(processResourcesTask)

                    if (binary.compilation.platformType == KotlinPlatformType.wasm) {
                        copy.from(
                            binary.linkSyncTask.flatMap { linkSyncTask ->
                                linkSyncTask.destinationDirectory.map { destDir ->
                                    binary.linkTask.map { linkTask ->
                                        linkTask.compilerOptions.moduleName.map {
                                            destDir.resolve("$it.wasm")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    copy.into(binary.distribution.directory)
                }

                val webpackTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    task.description = "build webpack ${mode.name.toLowerCaseAsciiOnly()} bundle"
                    task.outputDirectory.fileValue(binary.distribution.directory).finalizeValueOnRead()


                    task.dependsOn(
                        distributeResourcesTask
                    )

                    task.dependsOn(binary.linkSyncTask)

                    task.commonConfigure(
                        binary = binary,
                        mode = mode,
                        inputFilesDirectory = task.project.provider { binary.linkSyncTask.get().destinationDirectory.get() },
                        entryModuleName = binary.linkTask.flatMap { it.compilerOptions.moduleName },
                        configurationActions = webpackTaskConfigurations,
                        nodeJs = nodeJs,
                        defaultArchivesName = archivesName,
                    )
                }

                val distributionTask = registerSubTargetTask<Task>(
                    disambiguateCamelCased(
                        if (binary.mode == KotlinJsBinaryMode.PRODUCTION) "" else binary.name,
                        DISTRIBUTION_TASK_NAME
                    )
                ) {
                    it.dependsOn(webpackTask)
                    it.dependsOn(distributeResourcesTask)

                    it.outputs.dir(project.newFileProperty { binary.distribution.directory })
                }

                if (mode == KotlinJsBinaryMode.PRODUCTION) {
                    assembleTaskProvider.dependsOn(distributionTask)
                    registerSubTargetTask<Task>(
                        disambiguateCamelCased(WEBPACK_TASK_NAME)
                    ) {
                        it.dependsOn(webpackTask)
                    }
                }
            }
    }

    private fun KotlinWebpack.commonConfigure(
        binary: JsIrBinary,
        mode: KotlinJsBinaryMode,
        inputFilesDirectory: Provider<File>,
        entryModuleName: Provider<String>,
        configurationActions: DomainObjectSet<Action<KotlinWebpack>>,
        nodeJs: NodeJsRootExtension,
        defaultArchivesName: Property<String>,
    ) {
        dependsOn(
            nodeJs.npmInstallTaskProvider,
            nodeJs.storeYarnLockTaskProvider,
            target.project.tasks.named(compilation.processResourcesTaskName)
        )

        configureOptimization(mode)

        this.inputFilesDirectory.fileProvider(inputFilesDirectory)

        val platformType = binary.compilation.platformType
        val moduleKind = binary.linkTask.flatMap { it.compilerOptions.moduleKind }

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
        debugValue: T
    ): T = when (kind) {
        KotlinJsBinaryMode.PRODUCTION -> releaseValue
        KotlinJsBinaryMode.DEVELOPMENT -> debugValue
    }

    companion object {
        private const val WEBPACK_TASK_NAME = "webpack"
    }
}