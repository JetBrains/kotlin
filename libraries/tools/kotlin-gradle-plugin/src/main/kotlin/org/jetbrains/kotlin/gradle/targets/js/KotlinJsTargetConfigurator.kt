/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

open class KotlinJsTargetConfigurator(kotlinPluginVersion: String) :
    KotlinTargetConfigurator<KotlinJsCompilation>(true, true, kotlinPluginVersion) {

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(compilation.target.project, tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun configureCompilations(platformTarget: KotlinOnlyTarget<KotlinJsCompilation>) {
        super.configureCompilations(platformTarget)

        platformTarget.compilations.all {
            platformTarget.project.npmProject.configureCompilation(it)

            it.compileKotlinTask.kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target.compilations.all { compilation ->
            if (isTestCompilation(compilation)) {
                newTestsConfigurator(compilation).configure()
            }
        }
    }

    internal open fun newTestsConfigurator(compilation: KotlinJsCompilation) =
        KotlinJsCompilationTestsConfigurator(compilation)

    companion object {
        internal fun isTestCompilation(it: KotlinJsCompilation) =
            it.name == KotlinCompilation.TEST_COMPILATION_NAME
    }
}

class KotlinJsSingleTargetConfigurator(kotlinPluginVersion: String) :
    KotlinJsTargetConfigurator(kotlinPluginVersion) {

    override fun configureTarget(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        super.configureTarget(target)
        configureApplication(target)
    }

    private fun configureApplication(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target.compilations.all {
            if (it.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                KotlinWebpack.configure(it)
            }
        }
    }

    override fun newTestsConfigurator(compilation: KotlinJsCompilation) =
        object : KotlinJsCompilationTestsConfigurator(compilation) {
            override fun configureDefaultTestFramework(it: KotlinJsTest) {
                it.useMocha { }
            }
        }
}