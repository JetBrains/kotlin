/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinBrowserJs(
    target: KotlinOnlyTarget<KotlinJsCompilation>
) : KotlinJsInnerTargetConfigurator(target) {
    private val versions = project.nodeJs.versions

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useKarma {
            useWebpack()
            useChromeHeadless()
        }
    }

    override fun configureRun(compilation: KotlinJsCompilation) {
        val project = compilation.target.project
        val npmProject = project.npmProject
        val compileKotlinTask = compilation.compileKotlinTask

        compilation.dependencies {
            runtimeOnly(versions.webpack)
            runtimeOnly(versions.webpackCli)
            runtimeOnly(versions.webpackBundleAnalyzer)

            // for source map support
            runtimeOnly(versions.sourceMapLoader.npm(project))
            runtimeOnly(versions.sourceMapSupport.npm(project))
        }

        project.createOrRegisterTask<KotlinWebpack>(disambiguateCamelCased("webpack")) {
            it.dependsOn(compileKotlinTask)

            it.entry = npmProject.compileOutput(compileKotlinTask)

            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(it)
        }

        compilation.dependencies {
            runtimeOnly(npm("webpack-dev-server", "3.3.1"))
        }

        project.createOrRegisterTask<KotlinWebpack>(disambiguateCamelCased("run")) {
            it.dependsOn(compileKotlinTask)

            it.bin = "webpack-dev-server"
            it.entry = npmProject.compileOutput(compileKotlinTask)

            val projectDir = target.project.projectDir.canonicalPath
            it.devServer = KotlinWebpackConfig.DevServer(
                contentBase = listOf("$projectDir/src/main/resources")
            )

            it.outputs.upToDateWhen { false }
            project.tasks.maybeCreate("run").dependsOn(it)
        }
    }
}