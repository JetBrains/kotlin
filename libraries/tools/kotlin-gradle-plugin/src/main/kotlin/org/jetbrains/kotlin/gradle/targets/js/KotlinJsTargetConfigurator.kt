/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

open class KotlinJsTargetConfigurator(kotlinPluginVersion: String) :
    KotlinTargetConfigurator<KotlinJsCompilation>(true, true, kotlinPluginVersion) {

    override fun configureTarget(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target as KotlinJsTarget

        target.configureDefaults()
        super.configureTarget(target)

        target.compilations.forEach {
            it.compileKotlinTask.dependsOn(target.npmResolveTaskHolder.getTaskOrProvider())
        }

        if (target.disambiguationClassifier != null) {
            target.project.tasks.maybeCreate(runTaskNameSuffix).dependsOn(target.runTask)
        }
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        // tests configured in KotlinJsSubTarget.configure
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