/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDceDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion.Companion.choose
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.doNotTrackStateCompat
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import java.io.File
import javax.inject.Inject

abstract class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

    private val webpackTaskConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val runTaskConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()

    private val propertiesProvider = PropertiesProvider(project)
    private val webpackMajorVersion
        get() = propertiesProvider.webpackMajorVersion

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
                useChromeHeadless()
            }
        }

        if (test.enabled) {
            nodeJs.taskRequirements.addTaskRequirements(test)
        }
    }

    override fun commonWebpackConfig(body: KotlinWebpackConfig.() -> Unit) {
        webpackTaskConfigurations.add {
            webpackConfigApplier(body)
        }
        runTaskConfigurations.add {
            webpackConfigApplier(body)
        }
        testTask {
            onTestFrameworkSet {
                if (it is KotlinKarma) {
                    it.webpackConfig.body()
                }
            }
        }
    }

    override fun runTask(body: KotlinWebpack.() -> Unit) {
        runTaskConfigurations.add(body)
    }

    override fun webpackTask(body: KotlinWebpack.() -> Unit) {
        webpackTaskConfigurations.add(body)
    }

    @ExperimentalDceDsl
    override fun dceTask(body: KotlinJsDce.() -> Unit) {
        project.logger.warn("dceTask configuration is useless with IR compiler. Use @JsExport on declarations instead.")
    }

    override fun configureRun(
        compilation: KotlinJsIrCompilation
    ) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val commonRunTask = registerSubTargetTask<Task>(disambiguateCamelCased(RUN_TASK_NAME)) {}

        compilation.binaries
            .matching { it is Executable }
            .all { binary ->
                binary as Executable

                val mode = binary.mode

                val runTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    task.dependsOn(binary.linkSyncTask)
                    val entryFileProvider = binary.linkSyncTask.flatMap { syncTask ->
                        binary.linkTask.map {
                            syncTask.destinationDir.resolve(it.outputFileProperty.get().name)
                        }
                    }

                    webpackMajorVersion.choose(
                        { task.args.add(0, "serve") },
                        { task.bin = "webpack-dev-server/bin/webpack-dev-server.js" }
                    )()
                    task.description = "start ${mode.name.toLowerCase()} webpack dev server"

                    webpackMajorVersion.choose(
                        {
                            task.devServer = KotlinWebpackConfig.DevServer(
                                open = true,
                                static = mutableListOf(compilation.output.resourcesDir.canonicalPath),
                                client = KotlinWebpackConfig.DevServer.Client(
                                    KotlinWebpackConfig.DevServer.Client.Overlay(
                                        errors = true,
                                        warnings = false
                                    )
                                )
                            )
                        },
                        {
                            task.devServer = KotlinWebpackConfig.DevServer(
                                open = true,
                                contentBase = mutableListOf(compilation.output.resourcesDir.canonicalPath)
                            )
                        }
                    )()


                    task.doNotTrackStateCompat("Tracked by external webpack tool")

                    task.commonConfigure(
                        compilation = compilation,
                        mode = mode,
                        entryFileProvider = entryFileProvider,
                        configurationActions = runTaskConfigurations,
                        nodeJs = nodeJs
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
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val processResourcesTask = target.project.tasks.named(compilation.processResourcesTaskName)

        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

        compilation.binaries
            .matching { it is Executable }
            .all { binary ->
                binary as Executable

                val mode = binary.mode

                val distributeResourcesTask = registerSubTargetTask<Copy>(
                    disambiguateCamelCased(
                        binary.name,
                        DISTRIBUTE_RESOURCES_TASK_NAME
                    )
                ) {
                    it.from(processResourcesTask)
                    it.into(binary.distribution.directory)
                }

                val webpackTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    val entryFileProvider = binary.linkSyncTask.zip(binary.linkTask) { sync, link ->
                        sync.destinationDir
                            .resolve(link.compilerOptions.moduleName.get() + ".js")
                    }

                    task.description = "build webpack ${mode.name.toLowerCase()} bundle"
                    task._destinationDirectory = binary.distribution.directory

                    BuildMetricsService.registerIfAbsent(project)?.let {
                        task.buildMetricsService.value(it)
                    }

                    task.dependsOn(
                        distributeResourcesTask
                    )

                    task.commonConfigure(
                        compilation = compilation,
                        mode = mode,
                        entryFileProvider = entryFileProvider,
                        configurationActions = webpackTaskConfigurations,
                        nodeJs = nodeJs
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
        compilation: KotlinJsCompilation,
        mode: KotlinJsBinaryMode,
        entryFileProvider: Provider<File>,
        configurationActions: List<KotlinWebpack.() -> Unit>,
        nodeJs: NodeJsRootExtension
    ) {
        dependsOn(
            nodeJs.npmInstallTaskProvider,
            nodeJs.storeYarnLockTaskProvider,
            target.project.tasks.named(compilation.processResourcesTaskName)
        )

        configureOptimization(mode)

        entryProperty.fileProvider(entryFileProvider)

        configurationActions.forEach { configure ->
            configure()
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