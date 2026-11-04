/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.effectiveModuleKind
import org.jetbrains.kotlin.gradle.plugin.mpp.effectiveTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.swc.GenerateSwcConfig
import org.jetbrains.kotlin.gradle.targets.js.typescript.KotlinJsDtsGenerationTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

/**
 * package.json is generated for the module kind and target of the compilation,
 * so setting them differently on the tasks of the compilation (link, TypeScript declarations generation, webpack and SWC)
 * is deprecated.
 */
internal object JsTaskCompilerOptionMismatchChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val jsTargets = multiplatformExtension?.targets?.withType(KotlinJsIrTarget::class.java) ?: return

        // The compilation's options are also configured by its compile task, which may be realised after the link task.
        // Once the task graph is ready, all the tasks in it are realised and configured.
        project.gradle.taskGraph.whenReady { taskGraph ->
            val tasksInGraph = taskGraph.allTasks.filter { it.project == project }

            val compilations = jsTargets
                // Wasm always generates ES modules
                .filter { it.platformType == KotlinPlatformType.js }
                .flatMap { it.compilations }

            val optionValues = compilations.flatMap { compilation -> optionValues(compilation, tasksInGraph) }

            val mismatches = optionValues
                .mapNotNull { (taskPath, optionName, taskValue, compilationValue) ->
                    if (taskValue == null || compilationValue == null || taskValue == compilationValue) return@mapNotNull null
                    KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch.Mismatch(
                        taskPath = taskPath,
                        optionName = optionName,
                        taskValue = taskValue,
                        compilationValue = compilationValue,
                    )
                }
                .sortedWith(compareBy({ it.taskPath }, { it.optionName }))

            if (mismatches.isNotEmpty()) {
                collector.report(diagnosticsContext, KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch(mismatches))
            }
        }
    }

    /**
     * Describes the [option][optionName] for a [task][taskPath],
     * comparing the value from the task to the value from the compilation.
     *
     * The values aren't aligned then we raise a diagnostic.
     */
    private data class OptionValue(
        val taskPath: String,
        val optionName: String,
        val taskValue: String?,
        val compilationValue: String?,
    )

    /**
     * The `moduleKind` and `target` values of the compilation's tasks in [tasks], next to the compilation's ones.
     */
    private fun optionValues(compilation: KotlinJsIrCompilation, tasks: List<Task>): List<OptionValue> {
        val compilationModuleKind = compilation.jsModuleKind.map { it.kind }.orNull
        val compilationTarget = compilation.jsTarget.orNull

        fun compilerOptionValues(task: Task, compilerOptions: KotlinJsCompilerOptions) = listOf(
            OptionValue(task.path, "moduleKind", compilerOptions.effectiveModuleKind.map { it.kind }.orNull, compilationModuleKind),
            OptionValue(task.path, "target", compilerOptions.effectiveTarget.orNull, compilationTarget),
        )

        val binaryTaskNames = compilation.binaries.withType(JsIrBinary::class.java)
            .flatMap { listOf(it.linkTaskName, it.dtsGenerationTaskName) }
            .toSet()

        return tasks.flatMap { task ->
            when {
                task is KotlinJsIrLink && task.name in binaryTaskNames ->
                    compilerOptionValues(task, task.compilerOptions)

                task is KotlinJsDtsGenerationTask && task.name in binaryTaskNames ->
                    compilerOptionValues(task, task.linkCompilerOptions)

                task is KotlinWebpack && task.compilation == compilation ->
                    listOf(
                        OptionValue(
                            task.path,
                            "esModules",
                            task.esModules.orNull?.toString(),
                            compilation.jsModuleKind.map { it == JsModuleKind.MODULE_ES }.orNull?.toString(),
                        ),
                    )

                task is GenerateSwcConfig &&
                        task.targetAndCompilationName.orNull == compilation.target.name + " " + compilation.name ->
                    listOf(
                        OptionValue(task.path, "moduleKind", task.moduleSystemToUse.map { it.type }.orNull, compilationModuleKind),
                        OptionValue(task.path, "target", task.esTarget.orNull, compilationTarget),
                    )

                else -> emptyList()
            }
        }
    }
}
