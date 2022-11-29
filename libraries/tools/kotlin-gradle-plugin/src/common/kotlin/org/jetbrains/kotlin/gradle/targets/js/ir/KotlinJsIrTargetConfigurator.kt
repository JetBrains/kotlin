/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName

open class KotlinJsIrTargetConfigurator() :
    KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget>(true),
    KotlinTargetWithTestsConfigurator<KotlinJsReportAggregatingTestRun, KotlinJsIrTarget> {

    override val testRunClass: Class<KotlinJsReportAggregatingTestRun> get() = KotlinJsReportAggregatingTestRun::class.java

    override val archiveType: String
        get() = KLIB_TYPE

    override fun createTestRun(
        name: String,
        target: KotlinJsIrTarget
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

    override fun buildCompilationProcessor(compilation: KotlinJsIrCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider()
        return KotlinJsIrSourceSetProcessor(tasksProvider, KotlinCompilationInfo(compilation))
    }

    override fun createArchiveTasks(target: KotlinJsIrTarget): TaskProvider<out Zip> {
        return super.createArchiveTasks(target).apply {
            configure { it.archiveExtension.set(KLIB_TYPE) }
        }
    }

    override fun configureCompilations(target: KotlinJsIrTarget) {
        super.configureCompilations(target)

        target.compilations.all { compilation ->
            compilation.compilerOptions.configure {
                configureOptions()
                
                if (target.platformType == KotlinPlatformType.wasm) {
                    freeCompilerArgs.add(WASM_BACKEND)
                }

                freeCompilerArgs.add(DISABLE_PRE_IR)
            }

            compilation.binaries
                .withType(JsIrBinary::class.java)
                .all { binary ->
                    binary.linkTask.configure { linkTask ->
                        linkTask.compilerOptions.configureOptions()
                    }
                }
        }
    }

    private fun KotlinJsCompilerOptions.configureOptions() {
        moduleKind.set(JsModuleKind.MODULE_UMD)
        sourceMap.set(true)
        sourceMapEmbedSources.set(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_NEVER)
    }

    override fun defineConfigurationsForTarget(target: KotlinJsIrTarget) {
        super.defineConfigurationsForTarget(target)
        implementationToApiElements(target)

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
