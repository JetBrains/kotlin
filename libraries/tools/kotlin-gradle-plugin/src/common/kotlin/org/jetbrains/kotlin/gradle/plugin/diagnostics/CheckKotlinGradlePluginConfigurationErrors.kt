/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.KmpPartiallyResolvedDependenciesCheckerProjectsEvaluated
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.isPartiallyResolvedDependenciesCheckerEnabled
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.locateOrRegisterPartiallyResolvedDependenciesCheckerTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.withType

@DisableCachingByDefault(
    because = "This task renders reported diagnostics; caching this task will hide diagnostics and obscure issues in the build"
)
internal abstract class CheckKotlinGradlePluginConfigurationErrors : DefaultTask() {
    @get:Input
    abstract val errorDiagnostics: ListProperty<ToolingDiagnostic>

    @get:Input
    abstract val strongWarningDiagnostics: ListProperty<ToolingDiagnostic>

    @get:Internal
    abstract val renderingOptions: Property<ToolingDiagnosticRenderingOptions>

    @get:Internal
    abstract val problemsReporter: Property<ProblemsReporter>

    @TaskAction
    fun checkNoErrors() {
        val errorDiagnostics = errorDiagnostics.get()
        val strongWarningDiagnostics = strongWarningDiagnostics.get()
        val reporter = problemsReporter.get()
        val options = renderingOptions.get()
        if (strongWarningDiagnostics.isNotEmpty()) {
            strongWarningDiagnostics.reportProblems(reporter, options)
        }
        if (errorDiagnostics.isNotEmpty()) {
            errorDiagnostics.reportProblems(reporter, options)
            throw InvalidUserCodeException("Kotlin Gradle Plugin reported errors. Check the log for details")
        }
    }

    companion object {
        internal const val TASK_NAME = "checkKotlinGradlePluginConfigurationErrors"
    }
}

private const val DESCRIPTION =
    "Checks that Kotlin Gradle Plugin hasn't reported project configuration errors, failing otherwise. " +
            "This task always runs before compileKotlin* or similar tasks."

internal fun Project.locateOrRegisterCheckKotlinGradlePluginErrorsTask(): TaskProvider<CheckKotlinGradlePluginConfigurationErrors> {
    val partiallyResolvedDependenciesCheckerProjectsEvaluated = if (project.isPartiallyResolvedDependenciesCheckerEnabled) {
        locateOrRegisterPartiallyResolvedDependenciesCheckerTask()
    } else null
    val taskProvider = tasks.register(
        CheckKotlinGradlePluginConfigurationErrors.TASK_NAME,
        CheckKotlinGradlePluginConfigurationErrors::class.java,
    ) { task ->
        partiallyResolvedDependenciesCheckerProjectsEvaluated?.let {
            task.dependsOn(it)
        }
        task.errorDiagnostics.set(
            provider {
                kotlinToolingDiagnosticsCollector
                    .getDiagnosticsForProject(path)
                    .filter { it.severity == ToolingDiagnostic.Severity.ERROR }
            }
        )
        task.strongWarningDiagnostics.set(
            provider {
                kotlinToolingDiagnosticsCollector
                    .getDiagnosticsForProject(path)
                    .filter { it.severity == ToolingDiagnostic.Severity.STRONG_WARNING }
            }
        )
        task.usesService(kotlinToolingDiagnosticsCollectorProvider)
        task.problemsReporter.set(kotlinToolingDiagnosticsCollectorProvider.map { it.problemsReporter })
        task.renderingOptions.set(ToolingDiagnosticRenderingOptions.forProject(this))
        task.description = DESCRIPTION
        task.group = LifecycleBasePlugin.VERIFICATION_GROUP

        task.onlyIf("errorDiagnostics are present") {
            require(it is CheckKotlinGradlePluginConfigurationErrors)
            !it.errorDiagnostics.orNull.isNullOrEmpty() || !it.strongWarningDiagnostics.orNull.isNullOrEmpty()
        }
    }

    taskProvider.addDependsOnFromTasksThatShouldFailWhenErrorsReported(tasks)

    return taskProvider
}

/**
 * Adds dependsOn from some selection of the [tasks] to the [this]-task, effectively causing them to fail
 * if the ERROR-diagnostics were reported.
 *
 * Currently, we're doing it conservatively and instrumenting only [KotlinCompileTool]-tasks.
 * The intuition here is that if the build manages to do something useful for a user without compiling any .kt-sources,
 * then it's OK for KGP to let that build pass even if it reported ERROR-diagnostics.
 */
private fun TaskProvider<CheckKotlinGradlePluginConfigurationErrors>.addDependsOnFromTasksThatShouldFailWhenErrorsReported(tasks: TaskContainer) {
    tasks.withType<KotlinCompileTool>().configureEach { it.dependsOn(this) }
}
