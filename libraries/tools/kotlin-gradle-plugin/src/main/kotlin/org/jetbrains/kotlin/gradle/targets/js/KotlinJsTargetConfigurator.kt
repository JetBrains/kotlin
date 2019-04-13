/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask

class KotlinJsTargetConfigurator(kotlinPluginVersion: String) :
        KotlinTargetConfigurator<KotlinJsCompilation>(true, true, kotlinPluginVersion) {

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(compilation.target.project, tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun configureCompilations(platformTarget: KotlinOnlyTarget<KotlinJsCompilation>) {
        super.configureCompilations(platformTarget)

        platformTarget.compilations.all {
            it.compileKotlinTask.kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        val project = target.project
        val npmProject = NpmProjectLayout[project]

        target.compilations.all { compilation ->
            if (isTestCompilation(compilation)) {
                KotlinJsCompilationTestsConfigurator(compilation).configure()
            } else {
                KotlinWebpack.configure(compilation)
            }
        }
    }

    companion object {
        internal fun isTestCompilation(it: KotlinJsCompilation) =
            it.name == KotlinCompilation.TEST_COMPILATION_NAME
    }
}