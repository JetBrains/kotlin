/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfigWriter
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinBrowserJs(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

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

    override fun configureRun(compilation: KotlinJsCompilation) {
        val project = compilation.target.project
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

        project.createOrRegisterTask<KotlinWebpack>(disambiguateCamelCased("webpack")) {
            val compileKotlinTask = compilation.compileKotlinTask
            it.dependsOn(
                nodeJs.npmInstallTask,
                compileKotlinTask
            )

            it.compilation = compilation
            it.description = "build webpack bundle"

            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(it)
        }

        val run = project.createOrRegisterTask<KotlinWebpack>(disambiguateCamelCased("run")) {
            val compileKotlinTask = compilation.compileKotlinTask
            it.dependsOn(
                nodeJs.npmInstallTask,
                compileKotlinTask,
                target.project.tasks.getByName(compilation.processResourcesTaskName)
            )

            it.bin = "webpack-dev-server/bin/webpack-dev-server.js"
            it.compilation = compilation
            it.description = "start webpack dev server"

            it.devServer = KotlinWebpackConfigWriter.DevServer(
                open = true,
                contentBase = listOf(compilation.output.resourcesDir.canonicalPath)
            )

            it.outputs.upToDateWhen { false }
        }

        target.runTask.dependsOn(run.getTaskOrProvider())
    }
}