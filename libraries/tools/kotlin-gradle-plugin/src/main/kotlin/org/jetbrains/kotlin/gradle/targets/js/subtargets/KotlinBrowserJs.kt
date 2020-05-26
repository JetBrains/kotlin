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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce as KotlinJsDceTask

open class KotlinBrowserJs @Inject constructor(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val commonWebpackConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val commonRunConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val dceConfigurations: MutableList<KotlinJsDce.() -> Unit> = mutableListOf()
    private val distribution: Distribution = BrowserDistribution(project)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureDefaultTestFramework(testTask: KotlinJsTest) {
        testTask.useKarma {
            useChromeHeadless()
        }
    }

    override fun runTask(body: KotlinWebpack.() -> Unit) {
        commonRunConfigurations.add(body)
    }

    @ExperimentalDistributionDsl
    override fun distribution(body: Distribution.() -> Unit) {
        distribution.body()
    }

    override fun webpackTask(body: KotlinWebpack.() -> Unit) {
        commonWebpackConfigurations.add(body)
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

        val compileKotlinTask = compilation.compileKotlinTask

        val commonRunTask = registerSubTargetTask<Task>(disambiguateCamelCased(RUN_TASK_NAME)) {}

        compilation.binaries
            .all { binary ->
                val type = binary.type

                val runTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    )
                ) {
                    it.dependsOn(
                        nodeJs.npmInstallTask,
                        target.project.tasks.named(compilation.processResourcesTaskName)
                    )

                    it.configureOptimization(type)

                    it.bin = "webpack-dev-server/bin/webpack-dev-server.js"
                    it.compilation = compilation
                    it.description = "start ${type.name.toLowerCase()} webpack dev server"

                    it.devServer = KotlinWebpackConfig.DevServer(
                        open = true,
                        contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
                    )

                    it.outputs.upToDateWhen { false }

                    val actualDceTaskProvider = when (type) {
                        KotlinJsBinaryType.PRODUCTION -> dceTaskProvider
                        KotlinJsBinaryType.DEVELOPMENT -> devDceTaskProvider
                    }

                    // Breaking of Task Configuration Avoidance is not so critical
                    // because this task is dependent on DCE task
                    it.entry = actualDceTaskProvider.get()
                        .destinationDir
                        .resolve(compileKotlinTask.outputFile.name)

                    it.dependsOn(actualDceTaskProvider)

                    it.resolveFromModulesFirst = true

                    commonRunConfigurations.forEach { configure ->
                        it.configure()
                    }
                }

                if (type == KotlinJsBinaryType.DEVELOPMENT) {
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

        val compileKotlinTask = compilation.compileKotlinTask

        val processResourcesTask = target.project.tasks.named(compilation.processResourcesTaskName)

        val distributeResourcesTask = registerSubTargetTask<Copy>(
            disambiguateCamelCased(
                DISTRIBUTE_RESOURCES_TASK_NAME
            )
        ) {
            it.from(processResourcesTask)
            it.into(distribution.directory)
        }

        val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        assembleTask.dependsOn(distributeResourcesTask)

        compilation.binaries
            .all { binary ->
                val type = binary.type

                val webpackTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME

                    )
                ) {
                    it.dependsOn(
                        nodeJs.npmInstallTask,
                        processResourcesTask,
                        distributeResourcesTask
                    )

                    it.configureOptimization(type)

                    it.compilation = compilation
                    it.description = "build webpack ${type.name.toLowerCase()} bundle"
                    it._destinationDirectory = distribution.directory

                    val actualDceTaskProvider = when (type) {
                        KotlinJsBinaryType.PRODUCTION -> dceTaskProvider
                        KotlinJsBinaryType.DEVELOPMENT -> devDceTaskProvider
                    }

                    // Breaking of Task Configuration Avoidance is not so critical
                    // because this task is dependent on DCE task
                    it.entry = actualDceTaskProvider.get()
                        .destinationDir
                        .resolve(compileKotlinTask.outputFile.name)

                    it.dependsOn(actualDceTaskProvider)

                    commonWebpackConfigurations.forEach { configure ->
                        it.configure()
                    }
                }

                if (type == KotlinJsBinaryType.PRODUCTION) {
                    assembleTask.dependsOn(webpackTask)
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

        val kotlinTask = compilation.compileKotlinTask

        return project.registerTask(dceTaskName) {
            if (dev) {
                it.dceOptions.devMode = true
            } else {
                dceConfigurations.forEach { configure ->
                    it.configure()
                }
            }

            it.dependsOn(kotlinTask)

            it.kotlinFilesOnly = true

            it.classpath = project.configurations.getByName(compilation.runtimeDependencyConfigurationName)
            it.destinationDir = it.dceOptions.outputDirectory?.let { File(it) }
                ?: compilation.npmProject.dir.resolve(if (dev) DCE_DEV_DIR else DCE_DIR)

            it.source(kotlinTask.outputFile)
        }
    }

    private fun KotlinWebpack.configureOptimization(kind: KotlinJsBinaryType) {
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
        kind: KotlinJsBinaryType,
        releaseValue: T,
        debugValue: T
    ): T = when (kind) {
        KotlinJsBinaryType.PRODUCTION -> releaseValue
        KotlinJsBinaryType.DEVELOPMENT -> debugValue
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