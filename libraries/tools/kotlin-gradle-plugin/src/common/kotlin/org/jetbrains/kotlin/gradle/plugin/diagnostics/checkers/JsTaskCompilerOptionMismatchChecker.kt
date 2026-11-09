/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.effectiveModuleKind
import org.jetbrains.kotlin.gradle.plugin.mpp.effectiveTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.swc.GenerateSwcConfig
import org.jetbrains.kotlin.gradle.targets.js.typescript.KotlinJsDtsGenerationTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

/**
 * package.json is generated for the module kind and target of the compilation,
 * so setting them differently on the tasks of the compilation (link, TypeScript declarations generation, webpack and SWC)
 * breaks the build in unexpected ways.
 */
internal object JsTaskCompilerOptionMismatchChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val jsTargets = multiplatformExtension?.targets?.withType(KotlinJsIrTarget::class.java) ?: return

        // The compilation's options are also configured by its compile task, which may be realised after the link task.
        // Once the task graph is ready, all the tasks in it are realised and configured.
        project.gradle.taskGraph.whenReady {
            val compilations = jsTargets
                // ignore Wasm - it always generates ES modules
                .filter { it.platformType == KotlinPlatformType.js }
                .flatMap { it.compilations }

            val mismatches = compilations
                .flatMap { compilation ->
                    optionValues(compilation, project).mapNotNull { value ->
                        if (value.taskValue == null || value.expectedTaskValue == null || value.taskValue == value.expectedTaskValue) {
                            return@mapNotNull null
                        }
                        KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch.Mismatch(
                            compilationName = compilation.disambiguatedName,
                            taskPath = value.taskPath,
                            optionName = value.optionName,
                            taskValue = value.taskValue,
                            referenceTaskPath = value.referenceTaskPath,
                            referenceOptionName = value.referenceOptionName,
                            referenceValue = value.referenceValue ?: value.expectedTaskValue,
                        )
                    }
                }
                .sortedWith(compareBy({ it.compilationName }, { it.taskPath }, { it.optionName }))

            if (mismatches.isNotEmpty()) {
                collector.report(diagnosticsContext, KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch(mismatches))
            }
        }
    }

    /**
     * Describes the [option][optionName] for a [task][taskPath],
     * comparing the [value from the task][taskValue] to the [value it is expected to have][expectedTaskValue]
     * when it follows its [reference][referenceTaskPath]: its link task, or its compilation if `null`.
     *
     * If the values aren't aligned then we raise a diagnostic.
     */
    private data class OptionValue(
        val taskPath: String,
        val optionName: String,
        val taskValue: String?,
        val expectedTaskValue: String?,
        val referenceTaskPath: String? = null,
        val referenceOptionName: String = optionName,
        val referenceValue: String? = expectedTaskValue,
    )

    /**
     * The `moduleKind` and `target` values of the compilation's tasks.
     *
     * The link tasks are compared to the compilation.
     * The tasks following a link task (TypeScript declarations generation, webpack and SWC) are compared to their link task,
     * so a value set on a link task is reported once, where it is set.
     */
    private fun optionValues(compilation: KotlinJsIrCompilation, project: Project): List<OptionValue> {
        val compilationModuleKind = compilation.jsModuleKind.map { it.kind }.orNull
        val compilationTarget = compilation.jsTarget.orNull

        val binaries = compilation.binaries.withType(JsIrBinary::class.java)
        val binariesByLinkTaskName = binaries.associateBy { it.linkTaskName }
        val binariesByDtsGenerationTaskName = binaries.associateBy { it.dtsGenerationTaskName }

        fun JsIrBinary.linkTaskPath() = project.absoluteProjectPath(linkTaskName)

        fun linkTaskValues(task: Task, binary: JsIrBinary, moduleKind: String?, target: String?) = listOf(
            OptionValue(task.path, "moduleKind", moduleKind, binary.linkTaskModuleKind.map { it.kind }.orNull, binary.linkTaskPath()),
            OptionValue(task.path, "target", target, binary.linkTaskTarget.orNull, binary.linkTaskPath()),
        )

        val values = mutableListOf<OptionValue>()
        project.tasks.configureEach { task ->
            values += when {
                task is KotlinJsIrLink && task.name in binariesByLinkTaskName ->
                    listOf(
                        OptionValue(
                            task.path,
                            "moduleKind",
                            task.compilerOptions.effectiveModuleKind.map { it.kind }.orNull,
                            compilationModuleKind
                        ),
                        OptionValue(task.path, "target", task.compilerOptions.effectiveTarget.orNull, compilationTarget),
                    )

                task is KotlinJsDtsGenerationTask -> {
                    val binary = binariesByDtsGenerationTaskName[task.name] ?: return@configureEach
                    linkTaskValues(
                        task,
                        binary,
                        moduleKind = task.linkCompilerOptions.effectiveModuleKind.map { it.kind }.orNull,
                        target = task.linkCompilerOptions.effectiveTarget.orNull,
                    )
                }

                task is KotlinWebpack -> {
                    val esModules = task.esModules.orNull?.toString()
                    val binary = task.linkTaskName.orNull?.let(binariesByLinkTaskName::get)
                    when {
                        binary != null -> {
                            val linkTaskModuleKind = binary.linkTaskModuleKind.orNull
                            listOf(
                                OptionValue(
                                    task.path,
                                    "esModules",
                                    esModules,
                                    expectedTaskValue = linkTaskModuleKind?.let { it == JsModuleKind.MODULE_ES }?.toString(),
                                    referenceTaskPath = binary.linkTaskPath(),
                                    referenceOptionName = "moduleKind",
                                    referenceValue = linkTaskModuleKind?.kind,
                                )
                            )
                        }
                        // a custom webpack task, not following a link task
                        task.linkTaskName.orNull == null && task.compilation == compilation ->
                            listOf(
                                OptionValue(
                                    task.path,
                                    "esModules",
                                    esModules,
                                    expectedTaskValue = compilationModuleKind?.let { it == JsModuleKind.MODULE_ES.kind }?.toString(),
                                    referenceOptionName = "moduleKind",
                                    referenceValue = compilationModuleKind,
                                )
                            )
                        else -> emptyList()
                    }
                }

                task is GenerateSwcConfig -> {
                    val binary = task.linkTaskName.orNull?.let(binariesByLinkTaskName::get) ?: return@configureEach
                    linkTaskValues(
                        task,
                        binary,
                        moduleKind = task.moduleSystemToUse.map { it.type }.orNull,
                        target = task.esTarget.orNull,
                    )
                }

                else -> emptyList()
            }
        }
        return values
    }
}
