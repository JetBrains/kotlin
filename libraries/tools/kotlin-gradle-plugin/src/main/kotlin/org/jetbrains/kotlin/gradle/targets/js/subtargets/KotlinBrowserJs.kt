/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariant
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Devtool
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

open class KotlinBrowserJs @Inject constructor(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    override lateinit var buildVariants: NamedDomainObjectContainer<BuildVariant>

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside browser using karma and webpack"

    private val webpackTaskName = disambiguateCamelCased("webpack")

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useKarma {
            useChromeHeadless()
        }
    }

    override fun runTask(body: KotlinWebpack.() -> Unit) {
        (project.tasks.getByName(runTaskName) as KotlinWebpack).body()
    }

    override fun webpackTask(body: KotlinWebpack.() -> Unit) {
        (project.tasks.getByName(webpackTaskName) as KotlinWebpack).body()
    }

    override fun NamedDomainObjectContainer<BuildVariant>.release(body: BuildVariant.() -> Unit) {
        buildVariants.getByName(RELEASE).body()
    }

    override fun NamedDomainObjectContainer<BuildVariant>.debug(body: BuildVariant.() -> Unit) {
        buildVariants.getByName(DEBUG).body()
    }

    override fun configureRun(compilation: KotlinJsCompilation) {

        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        val run = project.registerTask<KotlinWebpack>(runTaskName) {
            val compileKotlinTask = compilation.compileKotlinTask
            it.dependsOn(
                nodeJs.npmInstallTask,
                compileKotlinTask,
                target.project.tasks.getByName(compilation.processResourcesTaskName)
            )

            it.bin = "webpack-dev-server/bin/webpack-dev-server.js"
            it.compilation = compilation
            it.description = "start webpack dev server"

            it.devServer = KotlinWebpackConfig.DevServer(
                open = true,
                contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
            )

            it.outputs.upToDateWhen { false }
        }

        target.runTask.dependsOn(run)
    }

    override fun configureBuild(compilation: KotlinJsCompilation) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        buildVariants.all { buildVariant ->
            project.registerTask<KotlinWebpack>(
                disambiguateCamelCased(
                    lowerCamelCaseName(
                        buildVariant.name,
                        "webpack"
                    )
                )
            ) {
                val compileKotlinTask = compilation.compileKotlinTask
                it.dependsOn(
                    nodeJs.npmInstallTask,
                    compileKotlinTask
                )

                val kind = buildVariant.kind

                it.mode = getByKind(
                    kind = kind,
                    releaseValue = Mode.PRODUCTION,
                    debugValue = Mode.DEVELOPMENT
                )

                it.devtool = getByKind(
                    kind = kind,
                    releaseValue = Devtool.SOURCE_MAP,
                    debugValue = Devtool.EVAL_SOURCE_MAP
                )

                it.compilation = compilation
                it.description = "build webpack bundle"

                project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(it)
            }
        }
    }

    private fun <T> getByKind(
        kind: BuildVariantKind,
        releaseValue: T,
        debugValue: T
    ): T = when (kind) {
        BuildVariantKind.RELEASE -> releaseValue
        BuildVariantKind.DEBUG -> debugValue
    }

    override fun configureBuildVariants() {
        buildVariants = project.container(BuildVariant::class.java)
        buildVariants.create(RELEASE) {
            it.kind = BuildVariantKind.RELEASE
        }
        buildVariants.create(DEBUG) {
            it.kind = BuildVariantKind.DEBUG
        }
    }

    companion object {
        const val RELEASE = "release"
        const val DEBUG = "debug"
    }
}