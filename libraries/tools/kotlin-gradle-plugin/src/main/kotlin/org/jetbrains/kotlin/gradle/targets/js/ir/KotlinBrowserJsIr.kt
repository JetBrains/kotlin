/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.subtargets.BrowserDistribution
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Devtool
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.tasks.registerTask
import javax.inject.Inject

open class KotlinBrowserJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "browser"),
    KotlinJsIrBrowserDsl {

    private val commonWebpackConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val commonRunConfigurations: MutableList<KotlinWebpack.() -> Unit> = mutableListOf()
    private val distribution: Distribution = BrowserDistribution()

    private lateinit var buildVariants: NamedDomainObjectContainer<BuildVariant>

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    override fun configure() {
        super.configure()
        browserProducingConfiguredHandlers.forEach { handler ->
            handler(this)
        }
    }

    override fun configureDefaultTestFramework(it: KotlinJsIrTest) {
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

    override fun configureMain(compilation: KotlinJsIrCompilation) {
        configureRun(compilation)
        configureBuild(compilation)
    }

    private fun configureRun(
        compilation: KotlinJsIrCompilation
    ) {

        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

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
                    getByKind(
                        kind,
                        compilation.productionLinkTask,
                        compilation.developmentLinkTask
                    ),
                    target.project.tasks.getByName(compilation.processResourcesTaskName)
                )

                it.configureOptimization(kind)

                it.bin = "webpack-dev-server/bin/webpack-dev-server.js"
                it.compilation = compilation
                it.entry = getByKind(
                    kind,
                    compilation.productionLinkTask.outputFile,
                    compilation.developmentLinkTask.outputFile
                )
                it.description = "start ${kind.name.toLowerCase()} webpack dev server"

                it.devServer = KotlinWebpackConfig.DevServer(
                    open = true,
                    contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
                )

                it.outputs.upToDateWhen { false }

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
        compilation: KotlinJsIrCompilation
    ) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val basePluginConvention = project.convention.plugins["base"] as BasePluginConvention?

        val baseDist = project.buildDir.resolve(basePluginConvention!!.distsDirName)
        distribution.directory = distribution.directory ?: baseDist

        val distributionTask = project.registerTask<Copy>(
            disambiguateCamelCased(
                DISTRIBUTION_TASK_NAME
            )
        ) {
            it.dependsOn(
                target.project.tasks.getByName(compilation.processResourcesTaskName)
            )

            it.from(compilation.output.resourcesDir)
            it.into(distribution.directory ?: baseDist)
        }

        val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        assembleTask.dependsOn(distributionTask)

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
                    getByKind(
                        kind,
                        compilation.productionLinkTask,
                        compilation.developmentLinkTask
                    ),
                    distributionTask
                )

                it.configureOptimization(kind)

                it.compilation = compilation
                it.entry = getByKind(
                    kind,
                    compilation.productionLinkTask.outputFile,
                    compilation.developmentLinkTask.outputFile
                )
                it.description = "build webpack ${kind.name.toLowerCase()} bundle"
                it.destinationDirectory = distribution.directory

                commonWebpackConfigurations.forEach { configure ->
                    it.configure()
                }
            }

            if (kind == BuildVariantKind.PRODUCTION) {
                assembleTask.dependsOn(webpackTask)
                project.registerTask<Task>(disambiguateCamelCased(WEBPACK_TASK_NAME)) {
                    it.dependsOn(webpackTask)
                }
            }
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

    fun whenProducingConfigured(body: KotlinBrowserJsIr.() -> Unit) {
        if (producingConfigured) {
            this.body()
        } else {
            browserProducingConfiguredHandlers += body
        }
    }

    companion object {
        const val PRODUCTION = "production"
        const val DEVELOPMENT = "development"

        private const val WEBPACK_TASK_NAME = "webpack"
        private const val DISTRIBUTION_TASK_NAME = "distribution"
    }
}