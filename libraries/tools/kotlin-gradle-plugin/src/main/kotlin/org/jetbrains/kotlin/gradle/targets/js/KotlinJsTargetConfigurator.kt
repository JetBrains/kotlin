/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTestsConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.defaultSourceSetName
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetConfigurator
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import java.util.concurrent.Callable

open class KotlinJsTargetConfigurator(kotlinPluginVersion: String, val irConfigurator: KotlinJsIrTargetConfigurator?) :
    KotlinOnlyTargetConfigurator<KotlinJsCompilation, KotlinJsTarget>(true, true, kotlinPluginVersion),
    KotlinTargetWithTestsConfigurator<KotlinJsReportAggregatingTestRun, KotlinJsTarget> {

    override val testRunClass: Class<KotlinJsReportAggregatingTestRun> get() = KotlinJsReportAggregatingTestRun::class.java

    override fun createTestRun(
        name: String,
        target: KotlinJsTarget
    ): KotlinJsReportAggregatingTestRun {
        val result = KotlinJsReportAggregatingTestRun(name, target)

        val testTask = target.project.kotlinTestRegistry.getOrCreateAggregatedTestTask(
            name = result.testTaskName,
            description = "Run JS tests for all platforms"
        )

        // workaround to avoid the infinite recursion in item factories of the target and the subtargets:
        target.testRuns.matching { it.name == name }.whenObjectAdded {
            it.configureAllExecutions {
                // do not do anything with the aggregated test run, but ensure that they are created
            }
        }

        result.executionTask = testTask

        return result
    }

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun configureCompilationDefaults(target: KotlinJsTarget) {
        val project = target.project

        target.compilations.all { compilation ->
            defineConfigurationsForCompilation(compilation)

            if (createDefaultSourceSets) {
                project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName).also { sourceSet ->
                    compilation.source(sourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
                }
            }

            configureResourceProcessing(compilation, project.files(Callable { compilation.allKotlinSourceSets.map { it.resources } }))

            createLifecycleTaskInternal(compilation)
        }
    }

    private fun createLifecycleTaskInternal(compilation: KotlinJsCompilation) {
        val project = compilation.target.project

        compilation.output.classesDirs.from(project.files().builtBy(compilation.compileAllTaskName))

        val compileAllTask = project.tasks.findByPath(compilation.compileAllTaskName)
        if (compileAllTask != null) {
            compileAllTask.dependsOn(compilation.compileKotlinTaskName)
            compileAllTask.dependsOn(compilation.processResourcesTaskName)
        } else {
            project.tasks.create(compilation.compileAllTaskName).apply {
                group = LifecycleBasePlugin.BUILD_GROUP
                description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
                dependsOn(compilation.compileKotlinTaskName)
                dependsOn(compilation.processResourcesTaskName)
            }
        }
    }

    override fun configureCompilations(target: KotlinJsTarget) {
        super.configureCompilations(target)

        target.compilations.all {
            it.compileKotlinTask.kotlinOptions {
                moduleKind = "umd"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }
}