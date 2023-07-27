/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.attributes.Usage
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.addSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import java.util.concurrent.Callable

open class KotlinJsTargetConfigurator :
    KotlinOnlyTargetConfigurator<KotlinJsCompilation, KotlinJsTarget>(true),
    KotlinTargetWithTestsConfigurator<KotlinJsReportAggregatingTestRun, KotlinJsTarget> {

    override val testRunClass: Class<KotlinJsReportAggregatingTestRun> get() = KotlinJsReportAggregatingTestRun::class.java

    override fun createTestRun(
        name: String,
        target: KotlinJsTarget
    ): KotlinJsReportAggregatingTestRun {
        val result = target.project.objects.newInstance(
            KotlinJsReportAggregatingTestRun::class.java,
            name,
            target
        )

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
        val tasksProvider = KotlinTasksProvider()
        return Kotlin2JsSourceSetProcessor(tasksProvider, KotlinCompilationInfo(compilation))
    }

    override fun configureCompilationDefaults(target: KotlinJsTarget) {
        val project = target.project

        target.compilations.all { compilation ->
            @Suppress("DEPRECATION")
            compilation.addSourceSet(compilation.defaultSourceSet)

            configureResourceProcessing(
                compilation,
                compilation.processResourcesTaskName,
                project.files(Callable { compilation.allKotlinSourceSets.map { it.resources } })
            )

            createLifecycleTaskInternal(compilation)
        }
    }

    private fun createLifecycleTaskInternal(compilation: KotlinJsCompilation) {
        val project = compilation.target.project

        compilation.output.classesDirs.from(project.files().builtBy(compilation.compileAllTaskName))

        val compileAllTask = project.locateTask<Task>(compilation.compileAllTaskName)
        if (compileAllTask != null) {
            compileAllTask.configure {
                it.dependsOn(compilation.compileKotlinTaskName)
                it.dependsOn(compilation.processResourcesTaskName)
            }
        } else {
            project.registerTask<DefaultTask>(compilation.compileAllTaskName) {
                it.group = LifecycleBasePlugin.BUILD_GROUP
                it.description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
                it.dependsOn(compilation.compileKotlinTaskName)
                it.dependsOn(compilation.processResourcesTaskName)
            }
        }
    }

    override fun configureCompilations(target: KotlinJsTarget) {
        super.configureCompilations(target)

        target.compilations.all {
            it.kotlinOptions {
                moduleKind = "umd"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }

    override fun defineConfigurationsForTarget(target: KotlinJsTarget) {
        super.defineConfigurationsForTarget(target)

        if (target.isMpp!!) return

        target.project.configurations.maybeCreate(
            target.commonFakeApiElementsConfigurationName
        ).apply {
            description = "Common Fake API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        }
    }
}