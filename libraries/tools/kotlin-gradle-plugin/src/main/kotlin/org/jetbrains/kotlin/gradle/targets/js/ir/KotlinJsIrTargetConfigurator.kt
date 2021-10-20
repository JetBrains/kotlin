/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.InvalidUserDataException
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import java.io.File

open class KotlinJsIrTargetConfigurator() :
    KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget>(true, true),
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
        val tasksProvider = KotlinTasksProvider()
        return KotlinJsIrSourceSetProcessor(tasksProvider, compilation)
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
                
                if (target.platformType == KotlinPlatformType.wasm) {
                    freeCompilerArgs = freeCompilerArgs + WASM_BACKEND
                }

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

                compilation.compileKotlinTaskProvider.configure { task ->
                    val outputFilePath = outputFile ?: if (produceUnzippedKlib) {
                        task.destinationDir.absoluteFile.normalize().absolutePath
                    } else {
                        File(task.destinationDir, "$baseName.$KLIB_TYPE").absoluteFile.normalize().absolutePath
                    }
                    outputFile = outputFilePath

                    val taskOutputDir = if (produceUnzippedKlib) File(outputFilePath) else File(outputFilePath).parentFile
                    if (taskOutputDir.isParentOf(task.project.rootDir))
                        throw InvalidUserDataException(
                            "The output directory '$taskOutputDir' (defined by outputFile of $task) contains or " +
                                    "matches the project root directory '${task.project.rootDir}'.\n" +
                                    "Gradle will not be able to build the project because of the root directory lock.\n" +
                                    "To fix this, consider using the default outputFile location instead of providing it explicitly."
                        )

                    task.destinationDir = taskOutputDir
                }

                val klibModuleName = target.project.klibModuleName(baseName)
                freeCompilerArgs = freeCompilerArgs + "$MODULE_NAME=$klibModuleName"
            }

            compilation.binaries
                .withType(JsIrBinary::class.java)
                .all { binary ->
                    binary.linkTask.configure { linkTask ->
                        linkTask.kotlinOptions.configureOptions()

                        val rootDir = binary.project.rootDir
                        linkTask.kotlinOptions.freeCompilerArgs += listOf(
                            "-source-map-base-dirs",
                            rootDir.absolutePath
                        )

                        linkTask.kotlinOptions.freeCompilerArgs += listOf(
                            "-source-map-prefix",
                            rootDir.toRelativeString(binary.compilation.npmProject.dist) + File.separator
                        )
                    }
                }
        }
    }

    private fun KotlinJsOptions.configureOptions() {
        moduleKind = "umd"
        sourceMap = true
        sourceMapEmbedSources = "never"
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
