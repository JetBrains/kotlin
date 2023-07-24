/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.UntrackedTask
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.withType

@DisableCachingByDefault(
    because = "This task renders reported diagnostics; caching this task will hiding this report and hide issues in the build"
)
internal abstract class EnsureNoKotlinGradlePluginErrors : DefaultTask() {
    @get:Input
    abstract val errorDiagnostics: MapProperty<String, Collection<ToolingDiagnostic>>

    @get:Internal
    abstract val renderingOptions: Property<ToolingDiagnosticRenderingOptions>

    @TaskAction
    fun checkNoErrors() {
        if (errorDiagnostics.get().isNotEmpty()) {
            renderAggregateReport(errorDiagnostics.get(), logger, renderingOptions.get().isVerbose)
            throw InvalidUserCodeException("Kotlin Gradle Plugin reported errors. Check the log for details")
        }
    }


    companion object {
        private const val TASK_NAME = "ensureNoKotlinGradlePluginErrors"

        internal fun register(
            tasks: TaskContainer,
            kotlinToolingDiagnosticsCollector: Provider<KotlinToolingDiagnosticsCollector>,
            toolingDiagnosticRenderingOptions: ToolingDiagnosticRenderingOptions,
        ) {
            tasks.register(TASK_NAME, EnsureNoKotlinGradlePluginErrors::class.java) { task ->
                task.errorDiagnostics.set(
                    kotlinToolingDiagnosticsCollector.get()
                        .getAllDiagnostics().asSequence()
                        .map { (project, diagnostics) ->
                            project to diagnostics.filter { it.severity == ToolingDiagnostic.Severity.ERROR }
                        }
                        .filter { (_, diagnostics) -> diagnostics.isNotEmpty() }
                        .toMap()
                )
                task.renderingOptions.set(toolingDiagnosticRenderingOptions)

                task.addDependsOnFromTasksThatShouldFailWhenErrorsReported(tasks)
            }
        }

        /**
         * Adds dependsOn from some selection of the [tasks] to the [this]-task, effectively causing them to fail
         * if the ERROR-diagnostics were reported.
         *
         * Currently, we're doing it conservatively and instrumenting only [KotlinCompileTool]-tasks.
         * The intuition here is that if the build manages to do something useful for a user without compiling any .kt-sources,
         * then it's OK for KGP to let that build pass even if it reported ERROR-diagnostics.
         */
        private fun EnsureNoKotlinGradlePluginErrors.addDependsOnFromTasksThatShouldFailWhenErrorsReported(tasks: TaskContainer) {
            tasks.withType<KotlinCompileTool>().all { it.dependsOn(this) }
        }
    }
}
