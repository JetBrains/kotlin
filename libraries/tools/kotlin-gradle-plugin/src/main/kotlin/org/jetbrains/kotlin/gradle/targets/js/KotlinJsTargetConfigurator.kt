/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

open class KotlinJsTargetConfigurator(kotlinPluginVersion: String) :
    KotlinTargetConfigurator<KotlinJsCompilation>(true, true, kotlinPluginVersion) {

    override fun configureTarget(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target as KotlinJsTarget

        super.configureTarget(target)

        target.compilations.forEach {
            it.compileKotlinTask.dependsOn(target.project.nodeJs.root.npmResolveTask)
        }
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target as KotlinJsTarget

        // always create jsTest task for compatibility (KT-31527)
        // actual tests tasks for browser and nodejs will be configured in KotlinJsSubTarget.configure
        target.testTask
    }

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(compilation.target.project, tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun configureCompilations(platformTarget: KotlinOnlyTarget<KotlinJsCompilation>) {
        super.configureCompilations(platformTarget)

        platformTarget.compilations.all {
            it.compileKotlinTask.kotlinOptions {
                moduleKind = "umd"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }
}