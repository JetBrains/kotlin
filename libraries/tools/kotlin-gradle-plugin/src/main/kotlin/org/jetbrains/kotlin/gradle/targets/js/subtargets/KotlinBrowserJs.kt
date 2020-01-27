/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Devtool
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
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
    private val distribution: Distribution = BrowserDistribution()

    private lateinit var buildVariants: NamedDomainObjectContainer<BuildVariant>

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun produceKotlinLibrary() {
        super.produceKotlinLibrary()
        target.irTarget?.browser?.produceKotlinLibrary()
    }

    override fun produceExecutable() {
        super.produceExecutable()
        target.irTarget?.browser?.produceExecutable()
    }

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
        val dceTaskProvider = configureDce(compilation)

        configureRun(compilation, dceTaskProvider)
        configureBuild(compilation, dceTaskProvider)
    }

    private fun configureRun(
        compilation: KotlinJsCompilation,
        dceTaskProvider: TaskProvider<KotlinJsDceTask>
    ) {

        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val compileKotlinTask = compilation.compileKotlinTask

        buildVariants.all { buildVariant ->
            val kind = buildVariant.kind
            val runTask = project.registerTask<KotlinWebpack>(
                disambiguateCamelCased(
                    buildVariant.name,
                    RUN_TASK_NAME
                )
            ) {
                it.dependsOn(
                    nodeJs.npmInstallTask,
                    target.project.tasks.named(compilation.processResourcesTaskName)
                )

                it.configureOptimization(kind)

                it.bin = "webpack-dev-server/bin/webpack-dev-server.js"
                it.compilation = compilation
                it.description = "start ${kind.name.toLowerCase()} webpack dev server"

                it.devServer = KotlinWebpackConfig.DevServer(
                    open = true,
                    contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
                )

                it.outputs.upToDateWhen { false }

                when (kind) {
                    BuildVariantKind.PRODUCTION -> {
                        // Breaking of Task Configuration Avoidance is not so critical
                        // because this task is dependent on DCE task
                        it.entry = dceTaskProvider.get()
                            .destinationDir
                            .resolve(compileKotlinTask.outputFile.name)
                        it.resolveFromModulesFirst = true
                        it.dependsOn(dceTaskProvider)
                    }
                    BuildVariantKind.DEVELOPMENT -> {
                        it.dependsOn(compileKotlinTask)
                    }
                }

                commonRunConfigurations.forEach { configure ->
                    it.configure()
                }
            }

            if (kind == BuildVariantKind.DEVELOPMENT) {
                target.runTask.dependsOn(runTask)
                project.registerTask<Task>(disambiguateCamelCased(RUN_TASK_NAME)) {
                    it.dependsOn(runTask)
                }
            }
        }
    }

    private fun configureBuild(
        compilation: KotlinJsCompilation,
        dceTaskProvider: TaskProvider<KotlinJsDceTask>
    ) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val compileKotlinTask = compilation.compileKotlinTask

        val basePluginConvention = project.convention.plugins["base"] as BasePluginConvention?

        val baseDist = project.buildDir.resolve(basePluginConvention!!.distsDirName)
        distribution.directory = distribution.directory ?: baseDist

        val processResourcesTask = target.project.tasks.named(compilation.processResourcesTaskName)

        val distributeResourcesTask = project.registerTask<Copy>(
            disambiguateCamelCased(
                DISTRIBUTE_RESOURCES_TASK_NAME
            )
        ) {
            it.dependsOn(processResourcesTask)

            it.from(compilation.output.resourcesDir)
            it.into(distribution.directory ?: baseDist)
        }

        val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        assembleTask.dependsOn(distributeResourcesTask)

        buildVariants.all { buildVariant ->
            val kind = buildVariant.kind
            val webpackTask = project.registerTask<KotlinWebpack>(
                disambiguateCamelCased(
                    buildVariant.name,
                    WEBPACK_TASK_NAME

                )
            ) {
                it.dependsOn(
                    nodeJs.npmInstallTask,
                    processResourcesTask,
                    distributeResourcesTask
                )

                it.configureOptimization(kind)

                it.compilation = compilation
                it.description = "build webpack ${kind.name.toLowerCase()} bundle"
                it.destinationDirectory = distribution.directory

                when (kind) {
                    BuildVariantKind.PRODUCTION -> {
                        // Breaking of Task Configuration Avoidance is not so critical
                        // because this task is dependent on DCE task
                        it.entry = dceTaskProvider.get()
                            .destinationDir
                            .resolve(compileKotlinTask.outputFile.name)
                        it.resolveFromModulesFirst = true
                        it.dependsOn(dceTaskProvider)
                    }
                    BuildVariantKind.DEVELOPMENT -> {
                        it.dependsOn(compileKotlinTask)
                    }
                }

                commonWebpackConfigurations.forEach { configure ->
                    it.configure()
                }
            }

            if (kind == BuildVariantKind.PRODUCTION) {
                assembleTask.dependsOn(webpackTask)
                val webpackCommonTask = project.registerTask<Task>(
                    disambiguateCamelCased(WEBPACK_TASK_NAME)
                ) {
                    it.dependsOn(webpackTask)
                }
                project.registerTask<Task>(disambiguateCamelCased(DISTRIBUTION_TASK_NAME)) {
                    it.dependsOn(webpackCommonTask)
                    it.dependsOn(distributeResourcesTask)
                }
            }
        }
    }

    private fun configureDce(compilation: KotlinJsCompilation): TaskProvider<KotlinJsDceTask> {
        val project = compilation.target.project

        val dceTaskName = lowerCamelCaseName(
            DCE_TASK_PREFIX,
            compilation.target.disambiguationClassifier,
            compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            DCE_TASK_SUFFIX
        )

        val kotlinTask = compilation.compileKotlinTask

        return project.registerTask(dceTaskName) {
            dceConfigurations.forEach { configure ->
                it.configure()
            }

            it.dependsOn(kotlinTask)

            it.kotlinFilesOnly = true

            it.classpath = project.configurations.getByName(compilation.runtimeDependencyConfigurationName)
            it.destinationDir = it.dceOptions.outputDirectory?.let { File(it) }
                ?: compilation.npmProject.dir.resolve(DCE_DIR)

            it.source(kotlinTask.outputFile)
        }
    }

    private fun KotlinWebpack.configureOptimization(kind: BuildVariantKind) {
        mode = getByKind(
            kind = kind,
            releaseValue = Mode.PRODUCTION,
            debugValue = Mode.DEVELOPMENT
        )

        devtool = getByKind(
            kind = kind,
            releaseValue = Devtool.SOURCE_MAP,
            debugValue = Devtool.EVAL_SOURCE_MAP
        )
    }

    private fun <T> getByKind(
        kind: BuildVariantKind,
        releaseValue: T,
        debugValue: T
    ): T = when (kind) {
        BuildVariantKind.PRODUCTION -> releaseValue
        BuildVariantKind.DEVELOPMENT -> debugValue
    }

    override fun configureBuildVariants() {
        buildVariants = project.container(BuildVariant::class.java)
        buildVariants.create(PRODUCTION) {
            it.kind = BuildVariantKind.PRODUCTION
        }
        buildVariants.create(DEVELOPMENT) {
            it.kind = BuildVariantKind.DEVELOPMENT
        }
    }

    companion object {
        const val DCE_TASK_PREFIX = "processDce"
        const val DCE_TASK_SUFFIX = "kotlinJs"

        const val DCE_DIR = "kotlin-dce"

        const val PRODUCTION = "production"
        const val DEVELOPMENT = "development"

        private const val WEBPACK_TASK_NAME = "webpack"
        private const val DISTRIBUTE_RESOURCES_TASK_NAME = "distributeResources"
        private const val DISTRIBUTION_TASK_NAME = "distribution"
    }
}