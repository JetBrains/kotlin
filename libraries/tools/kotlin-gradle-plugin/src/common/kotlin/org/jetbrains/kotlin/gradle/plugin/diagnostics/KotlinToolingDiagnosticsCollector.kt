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
internal typealias GradleProjectPath = String

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

    fun getDiagnosticsForProject(project: Project): Collection<ToolingDiagnostic> {
        return getDiagnosticsForProject(project.path)
    }

    fun report(
        projectPath: GradleProjectPath,
        renderingOptions: ToolingDiagnosticRenderingOptions,
        diagnostic: ToolingDiagnostic,
        reportOnce: Boolean = false,
        key: ToolingDiagnosticId = diagnostic.id,
    ) {
        if (reportedIds.add(key) || !reportOnce) {
            handleDiagnostic(projectPath, renderingOptions, diagnostic)
        }
    }

    fun report(
        project: Project,
        diagnostic: ToolingDiagnostic,
        reportOnce: Boolean = false,
        key: ToolingDiagnosticId = diagnostic.id,
    ) {
        if (reportedIds.add(key) || !reportOnce) {
            handleDiagnostic(project.path, ToolingDiagnosticRenderingOptions.forProject(project), diagnostic)
        }
    }

    fun report(
        from: UsesKotlinToolingDiagnosticsParameters,
        diagnostic: ToolingDiagnostic,
        reportOnce: Boolean = false,
        key: ToolingDiagnosticId = diagnostic.id,
    ) {
        if (reportedIds.add(key) || !reportOnce) {
            val options = from.diagnosticRenderingOptions.get()
            val projectPath = from.projectPath.get()
            // Execution-phase reporters must render immediately: with configuration cache, Gradle can
            // deserialize a fresh BuildService instance where transparent mode is not yet enabled.
            handleDiagnostic(projectPath, options, diagnostic, forceRender = true)
        }
    }

    fun switchToTransparentMode() {
        isTransparent = true
    }

    private fun handleDiagnostic(
        projectPath: GradleProjectPath,
        options: ToolingDiagnosticRenderingOptions,
        diagnostic: ToolingDiagnostic,
        // Force immediate rendering for execution-phase reporters; configuration cache may
        // deserialize a fresh BuildService instance before transparent mode is enabled.
        forceRender: Boolean = false,
    ) {
        if (diagnostic.isSuppressed(options)) return

        rawDiagnosticsFromProject.compute(projectPath) { _, previousListIfAny ->
            previousListIfAny?.apply { add(diagnostic) } ?: mutableListOf(diagnostic)
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

internal fun Project.reportDiagnostic(diagnostic: ToolingDiagnostic) {
    kotlinToolingDiagnosticsCollector.report(this, diagnostic)
}

internal fun KotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
    projectPath: GradleProjectPath,
    renderingOptions: ToolingDiagnosticRenderingOptions,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(projectPath, renderingOptions, diagnostic, reportOnce = true, ":#$key")
}

internal fun KotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(
    fromProject: Project,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(fromProject, diagnostic, reportOnce = true, ":#$key")
}

internal fun KotlinToolingDiagnosticsCollector.reportOncePerGradleProject(
    projectPath: GradleProjectPath,
    renderingOptions: ToolingDiagnosticRenderingOptions,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(projectPath, renderingOptions, diagnostic, reportOnce = true, "${projectPath}#$key")
}

internal fun KotlinToolingDiagnosticsCollector.reportOncePerGradleProject(
    fromProject: Project,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(fromProject, diagnostic, reportOnce = true, "${fromProject.path}#$key")
}

internal fun Project.reportDiagnosticOncePerProject(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleProject(this, diagnostic, key)
}

internal fun Project.reportDiagnosticOncePerBuild(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(this, diagnostic, key)
}

@RequiresOptIn("Usage of immediate diagnostic reporting is discouraged. Please use the regular diagnostics pipeline.")
internal annotation class ImmediateDiagnosticReporting

@ImmediateDiagnosticReporting
internal fun reportDiagnosticImmediately(
    logger: Logger,
    renderingOptions: ToolingDiagnosticRenderingOptions,
    diagnostic: ToolingDiagnostic,
) {
    diagnostic.renderReportedDiagnostic(logger, renderingOptions)
}

@ImmediateDiagnosticReporting
internal fun Project.reportDiagnosticImmediately(diagnostic: ToolingDiagnostic) {
    reportDiagnosticImmediately(logger, ToolingDiagnosticRenderingOptions.forProject(this), diagnostic)
}
