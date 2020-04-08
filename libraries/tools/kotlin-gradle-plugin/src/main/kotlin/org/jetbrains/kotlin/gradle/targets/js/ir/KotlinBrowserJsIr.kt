/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.subtargets.BrowserDistribution
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import javax.inject.Inject

open class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val commonWebpackConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val commonRunConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val distribution: Distribution = BrowserDistribution(project)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useKarma {
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

                val type = binary.type

                val runTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    )
                ) {
                    it.dependsOn(
                        nodeJs.npmInstallTask,
                        binary.linkTask,
                        target.project.tasks.getByName(compilation.processResourcesTaskName)
                    )

                    it.configureOptimization(type)

                    it.bin = "webpack-dev-server/bin/webpack-dev-server.js"
                    it.compilation = compilation
                    it.entry = binary.linkTask.map { it.outputFile }.get()
                    it.description = "start ${type.name.toLowerCase()} webpack dev server"

                    it.devServer = KotlinWebpackConfig.DevServer(
                        open = true,
                        contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
                    )

                    it.outputs.upToDateWhen { false }

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

    override fun configureBuild(
        compilation: KotlinJsIrCompilation
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

        val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        assembleTask.dependsOn(distributeResourcesTask)

        compilation.binaries
            .matching { it is Executable }
            .all { binary ->
                binary as Executable

                val type = binary.type
                val webpackTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME
                    )
                ) {
                    it.dependsOn(
                        nodeJs.npmInstallTask,
                        binary.linkTask,
                        distributeResourcesTask
                    )

                    it.configureOptimization(type)

                    it.compilation = compilation
                    it.entry = binary.linkTask.map { it.outputFile }.get()
                    it.description = "build webpack ${type.name.toLowerCase()} bundle"
                    it._destinationDirectory = distribution.directory

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
        private const val WEBPACK_TASK_NAME = "webpack"
        private const val DISTRIBUTE_RESOURCES_TASK_NAME = "distributeResources"
        private const val DISTRIBUTION_TASK_NAME = "distribution"
    }
}