/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

class KotlinJsTargetConfigurator(kotlinPluginVersion: String) :
        KotlinTargetConfigurator<KotlinJsCompilation>(true, true, kotlinPluginVersion) {

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(compilation.target.project, tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun configureCompilations(platformTarget: KotlinOnlyTarget<KotlinJsCompilation>) {
        super.configureCompilations(platformTarget)

        platformTarget.compilations.all {
            it.compileKotlinTask.kotlinOptions.moduleKind = "umd"
        }
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target.compilations.all {
            if (isTestCompilation(it)) {
                KotlinJsCompilationTestsConfigurator(it).configure()
            }
        }
    }

    companion object {
        internal fun isTestCompilation(it: KotlinJsCompilation) =
            it.name == KotlinCompilation.TEST_COMPILATION_NAME
    }
}