/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
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
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.decamelize
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import javax.inject.Inject

open class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val webpackTaskConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val runTaskConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useKarma {
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

                val runCompileSync = registerRunCompileSync(
                    binary
                )

                val runTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        RUN_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    val entryFileProvider = runCompileSync.map {
                        it.destinationDir
                            .resolve(binary.linkTask.get().outputFile.name)
                    }

                    task.bin = "webpack-dev-server/bin/webpack-dev-server.js"
                    task.description = "start ${mode.name.toLowerCase()} webpack dev server"

                    task.devServer = KotlinWebpackConfig.DevServer(
                        open = true,
                        contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
                    )

                    task.outputs.upToDateWhen { false }

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

        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

        compilation.binaries
            .matching { it is Executable }
            .all { binary ->
                binary as Executable

                val mode = binary.mode
                val webpackTask = registerSubTargetTask<KotlinWebpack>(
                    disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        WEBPACK_TASK_NAME
                    ),
                    listOf(compilation)
                ) { task ->
                    val entryFileProvider = binary.linkTask.map { it.outputFile }

                    task.description = "build webpack ${mode.name.toLowerCase()} bundle"
                    task._destinationDirectory = distribution.directory

                    task.commonConfigure(
                        compilation = compilation,
                        mode = mode,
                        entryFileProvider = entryFileProvider,
                        configurationActions = webpackTaskConfigurations,
                        nodeJs = nodeJs
                    )
                }

                val distributeResourcesTask = registerSubTargetTask<Copy>(
                    disambiguateCamelCased(
                        binary.name,
                        DISTRIBUTE_RESOURCES_TASK_NAME
                    )
                ) {
                    it.from(processResourcesTask)
                    it.into(binary.distribution.directory)
                }

                if (mode == KotlinJsBinaryMode.PRODUCTION) {
                    assembleTaskProvider.dependsOn(webpackTask)
                    val webpackCommonTask = registerSubTargetTask<Task>(
                        disambiguateCamelCased(WEBPACK_TASK_NAME)
                    ) {
                        it.dependsOn(webpackTask)
                    }

                    webpackTask.dependsOn(
                        distributeResourcesTask
                    )

                    assembleTaskProvider.dependsOn(distributeResourcesTask)

                    registerSubTargetTask<Task>(disambiguateCamelCased(DISTRIBUTION_TASK_NAME)) {
                        it.dependsOn(webpackCommonTask)
                        it.dependsOn(distributeResourcesTask)

                        it.outputs.dir(distribution.directory)
                    }
                }
            }
    }

    private fun registerRunCompileSync(binary: Executable): TaskProvider<Sync> {
        val compilation = binary.compilation
        val runCompileSyncTaskName = lowerCamelCaseName(
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            binary.name,
            RUN_COMPILE_COPY
        )

        return registerSubTargetTask(
            runCompileSyncTaskName
        ) { task ->
            task.from(
                project.layout.file(binary.linkTask.map { it.destinationDir })
            )

            task.into(
                binary.linkTask.map {
                    it.destinationDir.parentFile
                        .resolve(binary.name.decamelize())
                }
            )
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
            target.project.tasks.named(compilation.processResourcesTaskName)
        )

        configureOptimization(mode)

        entryProperty.set(
            project.layout.file(entryFileProvider)
        )

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

        private const val RUN_COMPILE_COPY = "runCompileSync"
    }
}