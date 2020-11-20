/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.executeTaskBaseName
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce as KotlinJsDceTask

open class KotlinBrowserJs @Inject constructor(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val webpackTaskConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val runTaskConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val dceConfigurations: MutableList<KotlinJsDce.() -> Unit> = mutableListOf()
    private val distribution: Distribution = DefaultDistribution(project)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureDefaultTestFramework(testTask: KotlinJsTest) {
        testTask.useKarma {
            useChromeHeadless()
        }
    }

    override fun commonWebpackConfig(body: KotlinWebpackConfig.() -> Unit) {
        webpackTaskConfigurations.add {
            webpackConfigAppliers.add(body)
        }
        runTaskConfigurations.add {
            webpackConfigAppliers.add(body)
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

    @ExperimentalDistributionDsl
    override fun distribution(body: Distribution.() -> Unit) {
        distribution.body()
    }

    override fun webpackTask(body: KotlinWebpack.() -> Unit) {
        webpackTaskConfigurations.add(body)
    }

    @ExperimentalDceDsl
    override fun dceTask(body: KotlinJsDce.() -> Unit) {
        dceConfigurations.add(body)
    }

    override fun configureMain(compilation: KotlinJsCompilation) {
        val dceTaskProvider = configureDce(
            compilation = compilation,
            dev = false
        )

        val devDceTaskProvider = configureDce(
            compilation = compilation,
            dev = true
        )

        configureRun(
            compilation = compilation,
            dceTaskProvider = dceTaskProvider,
            devDceTaskProvider = devDceTaskProvider
        )
        configureBuild(
            compilation = compilation,
            dceTaskProvider = dceTaskProvider,
            devDceTaskProvider = devDceTaskProvider
        )
    }

    private fun configureRun(
        compilation: KotlinJsCompilation,
        dceTaskProvider: TaskProvider<KotlinJsDceTask>,
        devDceTaskProvider: TaskProvider<KotlinJsDceTask>
    ) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val commonRunTask = registerSubTargetTask<Task>(disambiguateCamelCased(RUN_TASK_NAME)) {}

        compilation.binaries
            .all { binary ->
                val type = binary.mode

                val runTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    task.bin = "webpack-dev-server/bin/webpack-dev-server.js"
                    task.description = "start ${type.name.toLowerCase()} webpack dev server"

                    task.devServer = KotlinWebpackConfig.DevServer(
                        open = true,
                        contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
                    )

                    task.outputs.upToDateWhen { false }

                    task.commonConfigure(
                        compilation = compilation,
                        dceTaskProvider = dceTaskProvider,
                        devDceTaskProvider = devDceTaskProvider,
                        mode = type,
                        configurationActions = runTaskConfigurations,
                        nodeJs = nodeJs
                    )
                }

                if (type == KotlinJsBinaryMode.DEVELOPMENT) {
                    target.runTask.dependsOn(runTask)
                    commonRunTask.configure {
                        it.dependsOn(runTask)
                    }
                }
            }
    }

    private fun configureBuild(
        compilation: KotlinJsCompilation,
        dceTaskProvider: TaskProvider<KotlinJsDceTask>,
        devDceTaskProvider: TaskProvider<KotlinJsDceTask>
    ) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val processResourcesTask = target.project.tasks.named(compilation.processResourcesTaskName)

        val distributeResourcesTask = registerSubTargetTask<Copy>(
            disambiguateCamelCased(
                DISTRIBUTE_RESOURCES_TASK_NAME
            )
        ) {
            it.from(processResourcesTask)
            it.into(distribution.directory)
        }

        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        assembleTaskProvider.dependsOn(distributeResourcesTask)

        compilation.binaries
            .all { binary ->
                val type = binary.mode

                val webpackTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME

                    ),
                    listOf(compilation)
                ) { task ->
                    task.dependsOn(
                        distributeResourcesTask
                    )

                    task.description = "build webpack ${type.name.toLowerCase()} bundle"
                    task._destinationDirectory = distribution.directory

                    task.commonConfigure(
                        compilation = compilation,
                        dceTaskProvider = dceTaskProvider,
                        devDceTaskProvider = devDceTaskProvider,
                        mode = type,
                        configurationActions = webpackTaskConfigurations,
                        nodeJs = nodeJs
                    )
                }

                if (type == KotlinJsBinaryMode.PRODUCTION) {
                    assembleTaskProvider.dependsOn(webpackTask)
                    val webpackCommonTask = registerSubTargetTask<Task>(
                        disambiguateCamelCased(WEBPACK_TASK_NAME)
                    ) {
                        it.dependsOn(webpackTask)
                    }
                    registerSubTargetTask<Task>(disambiguateCamelCased(DISTRIBUTION_TASK_NAME)) {
                        it.dependsOn(webpackCommonTask)
                        it.dependsOn(distributeResourcesTask)

                        it.outputs.dir(distribution.directory)
                    }
                }
            }
    }

    private fun KotlinWebpack.commonConfigure(
        compilation: KotlinJsCompilation,
        dceTaskProvider: TaskProvider<KotlinJsDceTask>,
        devDceTaskProvider: TaskProvider<KotlinJsDceTask>,
        mode: KotlinJsBinaryMode,
        configurationActions: List<KotlinWebpack.() -> Unit>,
        nodeJs: NodeJsRootExtension
    ) {
        dependsOn(
            nodeJs.npmInstallTaskProvider,
            target.project.tasks.named(compilation.processResourcesTaskName)
        )

        configureOptimization(mode)

        val actualDceTaskProvider = when (mode) {
            KotlinJsBinaryMode.PRODUCTION -> dceTaskProvider
            KotlinJsBinaryMode.DEVELOPMENT -> devDceTaskProvider
        }

        entryProperty.set(
            project.layout.file(actualDceTaskProvider.map {
                it.destinationDir.resolve(compilation.compileKotlinTask.outputFile.name)
            })
        )

        resolveFromModulesFirst = true

        configurationActions.forEach { configure ->
            configure()
        }
    }

    private fun configureDce(
        compilation: KotlinJsCompilation,
        dev: Boolean
    ): TaskProvider<KotlinJsDceTask> {
        val project = compilation.target.project

        val dceTaskName = lowerCamelCaseName(
            DCE_TASK_PREFIX,
            if (dev) DCE_DEV_PART else null,
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            DCE_TASK_SUFFIX
        )

        val kotlinTask = compilation.compileKotlinTaskProvider

        return project.registerTask(dceTaskName) {
            if (dev) {
                it.dceOptions.devMode = true
            } else {
                dceConfigurations.forEach { configure ->
                    it.configure()
                }
            }

            it.kotlinFilesOnly = true

            it.classpath = project.configurations.getByName(compilation.runtimeDependencyConfigurationName)
            it.destinationDir = it.dceOptions.outputDirectory?.let { File(it) }
                ?: compilation.npmProject.dir.resolve(if (dev) DCE_DEV_DIR else DCE_DIR)

            it.source(kotlinTask.map { it.outputFile })
        }
    }

    private fun KotlinWebpack.configureOptimization(kind: KotlinJsBinaryMode) {
        mode = getByKind(
            kind = kind,
            releaseValue = Mode.PRODUCTION,
            debugValue = Mode.DEVELOPMENT
        )

        devtool = getByKind(
            kind = kind,
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
        const val DCE_TASK_PREFIX = "processDce"
        private const val DCE_DEV_PART = "dev"
        const val DCE_TASK_SUFFIX = "kotlinJs"

        const val DCE_DIR = "kotlin-dce"
        const val DCE_DEV_DIR = "kotlin-dce-dev"

        const val PRODUCTION = "production"
        const val DEVELOPMENT = "development"

        private const val WEBPACK_TASK_NAME = "webpack"
        private const val DISTRIBUTE_RESOURCES_TASK_NAME = "distributeResources"
        private const val DISTRIBUTION_TASK_NAME = "distribution"
    }
}