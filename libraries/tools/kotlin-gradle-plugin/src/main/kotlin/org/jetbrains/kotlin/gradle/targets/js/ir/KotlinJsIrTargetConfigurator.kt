/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinJsIrSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTestsConfigurator
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName

open class KotlinJsIrTargetConfigurator(kotlinPluginVersion: String) :
    KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget>(true, true, kotlinPluginVersion),
    KotlinTargetWithTestsConfigurator<KotlinJsReportAggregatingTestRun, KotlinJsIrTarget> {

    override val testRunClass: Class<KotlinJsReportAggregatingTestRun> get() = KotlinJsReportAggregatingTestRun::class.java

    override val archiveType: String
        get() = KLIB_TYPE

    override fun createTestRun(
        name: String,
        target: KotlinJsIrTarget
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

    override fun buildCompilationProcessor(compilation: KotlinJsIrCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return KotlinJsIrSourceSetProcessor(tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun createArchiveTasks(target: KotlinJsIrTarget): Zip {
        return super.createArchiveTasks(target).apply {
            // not archiveExtension because it is since Gradle 5.1 only
            extension = KLIB_TYPE
        }
    }

    override fun configureCompilations(target: KotlinJsIrTarget) {
        super.configureCompilations(target)

        target.compilations.all {
            it.compileKotlinTask.kotlinOptions {
                configureOptions()

                freeCompilerArgs += listOf(
                    DISABLE_PRE_IR,
                    PRODUCE_UNZIPPED_KLIB
                )
            }

            it.binaries
                .withType(JsIrBinary::class.java)
                .all {
                    it.linkTask.configure { linkTask ->
                        linkTask.kotlinOptions.configureOptions()
                    }
                }
        }
    }

    private fun KotlinJsOptions.configureOptions() {
        moduleKind = "umd"
        sourceMap = true
    }

    override fun defineConfigurationsForTarget(target: KotlinJsIrTarget) {
        super.defineConfigurationsForTarget(target)
        implementationToApiElements(target)
    }
}