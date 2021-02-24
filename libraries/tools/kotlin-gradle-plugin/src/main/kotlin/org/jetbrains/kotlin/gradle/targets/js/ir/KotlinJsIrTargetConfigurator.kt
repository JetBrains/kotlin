/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import java.io.File

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

    override fun createArchiveTasks(target: KotlinJsIrTarget): TaskProvider<out Zip> {
        return super.createArchiveTasks(target).apply {
            configure { it.archiveExtension.set(KLIB_TYPE) }
        }
    }

    override fun configureCompilations(target: KotlinJsIrTarget) {
        super.configureCompilations(target)

        target.compilations.all { compilation ->
            compilation.kotlinOptions {
                configureOptions()

                var produceUnzippedKlib = isProduceUnzippedKlib()
                val produceZippedKlib = isProduceZippedKlib()

                freeCompilerArgs = freeCompilerArgs + DISABLE_PRE_IR

                val isMainCompilation = compilation.isMain()

                if (!produceUnzippedKlib && !produceZippedKlib) {
                    freeCompilerArgs = freeCompilerArgs + PRODUCE_UNZIPPED_KLIB
                    produceUnzippedKlib = true
                }

                // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
                val baseName = if (isMainCompilation) {
                    target.project.name
                } else {
                    "${target.project.name}_${compilation.name}"
                }

                val destinationDir = compilation.compileKotlinTask.destinationDir
                outputFile = if (produceUnzippedKlib)
                    destinationDir.absoluteFile.normalize().absolutePath
                else
                    File(destinationDir, "$baseName.$KLIB_TYPE").absoluteFile.normalize().absolutePath

                val klibModuleName = target.project.klibModuleName(baseName)
                freeCompilerArgs = freeCompilerArgs + "$MODULE_NAME=$klibModuleName"
            }

            compilation.binaries
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