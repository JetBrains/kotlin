/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactoryProvider
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private typealias ToolingDiagnosticId = String
private typealias GradleProjectPath = String

internal abstract class KotlinToolingDiagnosticsCollector @Inject constructor(
    private val objects: ObjectFactory,
) : BuildService<KotlinToolingDiagnosticsCollector.Parameter> {
    interface Parameter : BuildServiceParameters {
        val problemsReporterFactory: Property<ProblemsReporter.Factory>
    }

    @get:Internal
    internal val problemsReporter get() = parameters.problemsReporterFactory.get().getInstance(objects)

    /**
     * When collector is in transparent mode, any diagnostics received will be immediately rendered
     * instead of collected
     */
    @Volatile
    private var isTransparent: Boolean = false

    private val rawDiagnosticsFromProject: MutableMap<GradleProjectPath, MutableList<ToolingDiagnostic>> = ConcurrentHashMap()
    private val reportedIds: MutableSet<ToolingDiagnosticId> = Collections.newSetFromMap(ConcurrentHashMap())

    fun getDiagnosticsForProject(projectPath: GradleProjectPath): Collection<ToolingDiagnostic> {
        return rawDiagnosticsFromProject[projectPath] ?: emptyList()
    }

    fun report(
        context: ToolingDiagnosticsContext,
        diagnostic: ToolingDiagnostic,
        reportOnce: Boolean = false,
        key: ToolingDiagnosticId = diagnostic.id,
    ) {
        if (reportedIds.add(key) || !reportOnce) {
            handleDiagnostic(context, diagnostic)
        }
    }

    fun report(
        from: UsesKotlinToolingDiagnosticsParameters,
        diagnostic: ToolingDiagnostic,
        reportOnce: Boolean = false,
        key: ToolingDiagnosticId = diagnostic.id,
    ) {
        if (reportedIds.add(key) || !reportOnce) {
            // Execution-phase reporters must render immediately: with configuration cache, Gradle can
            // deserialize a fresh BuildService instance where transparent mode is not yet enabled.
            handleDiagnostic(from.toolingDiagnosticsContext.get(), diagnostic, forceRender = true)
        }
    }

    fun switchToTransparentMode() {
        isTransparent = true
    }

    private fun handleDiagnostic(
        context: ToolingDiagnosticsContext,
        diagnostic: ToolingDiagnostic,
        // Force immediate rendering for execution-phase reporters; configuration cache may
        // deserialize a fresh BuildService instance before transparent mode is enabled.
        forceRender: Boolean = false,
    ) {
        val projectPath = context.projectPath
        val options = context.renderingOptions
        if (diagnostic.isSuppressed(options)) return

        if (!forceRender) {
            rawDiagnosticsFromProject.compute(projectPath) { _, previousListIfAny ->
                previousListIfAny?.apply { add(diagnostic) } ?: mutableListOf(diagnostic)
            }
        }

        if (isTransparent || forceRender) {
            problemsReporter.reportProblemDiagnostic(diagnostic, options)
            return
        }

        if (diagnostic.severity == ToolingDiagnostic.Severity.FATAL) {
            throw diagnostic.createAnExceptionForFatalDiagnostic(options)
        }
    }
}

internal val Project.kotlinToolingDiagnosticsCollectorProvider: Provider<KotlinToolingDiagnosticsCollector>
    get() = gradle.registerClassLoaderScopedBuildService(KotlinToolingDiagnosticsCollector::class) {
        it.parameters.problemsReporterFactory.set(variantImplementationFactoryProvider<ProblemsReporter.Factory>())
    }

internal val Project.kotlinToolingDiagnosticsCollector: KotlinToolingDiagnosticsCollector
    get() = kotlinToolingDiagnosticsCollectorProvider.get()

/**
 * Reports [diagnostic] through this project's [KotlinToolingDiagnosticsCollector].
 *
 * This method does not deduplicate diagnostics and reports each invocation independently.
 */
internal fun Project.reportDiagnostic(diagnostic: ToolingDiagnostic) {
    kotlinToolingDiagnosticsCollector.report(toolingDiagnosticsContext, diagnostic)
}

/**
 * Reports [diagnostic] at most once during the current Gradle build.
 *
 * Subsequent calls with the same [key] are ignored.
 */
internal fun KotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
    context: ToolingDiagnosticsContext,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(context, diagnostic, reportOnce = true, ":#$key")
}

/**
 * Reports [diagnostic] at most once for a given Gradle project.
 *
 * Deduplication is scoped to `context.projectPath`, so different projects can report the same [key].
 */
internal fun KotlinToolingDiagnosticsCollector.reportOncePerGradleProject(
    context: ToolingDiagnosticsContext,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(context, diagnostic, reportOnce = true, "${context.projectPath}#$key")
}

/**
 * Convenience wrapper for [KotlinToolingDiagnosticsCollector.reportOncePerGradleProject]
 * that uses this project's diagnostics context.
 */
internal fun Project.reportDiagnosticOncePerProject(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleProject(toolingDiagnosticsContext, diagnostic, key)
}

/**
 * Convenience wrapper for [KotlinToolingDiagnosticsCollector.reportOncePerGradleBuild]
 * that uses this project's diagnostics context.
 */
internal fun Project.reportDiagnosticOncePerBuild(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(toolingDiagnosticsContext, diagnostic, key)
}

@RequiresOptIn("Usage of immediate diagnostic reporting is discouraged. Please use the regular diagnostics pipeline.")
internal annotation class ImmediateDiagnosticReporting

/**
 * Renders [diagnostic] immediately using the provided logger and rendering options.
 */
@ImmediateDiagnosticReporting
internal fun reportDiagnosticImmediately(
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions,
    diagnostic: ToolingDiagnostic,
) {
    diagnostic.renderReportedDiagnostic(logger, renderingOptions)
}

/**
 * Convenience overload of [reportDiagnosticImmediately] that resolves logger and rendering options
 * from this [Project].
 */
@ImmediateDiagnosticReporting
internal fun Project.reportDiagnosticImmediately(diagnostic: ToolingDiagnostic) {
    reportDiagnosticImmediately(logger, ToolingDiagnosticRenderingOptions.forProject(this), diagnostic)
}
