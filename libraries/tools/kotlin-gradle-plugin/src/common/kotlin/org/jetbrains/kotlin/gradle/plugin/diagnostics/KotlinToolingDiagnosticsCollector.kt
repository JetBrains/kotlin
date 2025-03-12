/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
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

    fun getDiagnosticsForProject(project: Project): Collection<ToolingDiagnostic> {
        return rawDiagnosticsFromProject[project.path] ?: return emptyList()
    }

    fun report(
        project: Project,
        diagnostic: ToolingDiagnostic,
        reportOnce: Boolean = false,
        key: ToolingDiagnosticId = diagnostic.id,
    ) {
        if (reportedIds.add(key) || !reportOnce) {
            handleDiagnostic(project, diagnostic)
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
            problemsReporter.reportProblemDiagnostic(diagnostic, options)
        }
    }

    fun switchToTransparentMode() {
        isTransparent = true
    }

    private fun handleDiagnostic(project: Project, diagnostic: ToolingDiagnostic) {
        val options = ToolingDiagnosticRenderingOptions.forProject(project)
        if (diagnostic.isSuppressed(options)) return

        if (isTransparent) {
            problemsReporter.reportProblemDiagnostic(diagnostic, options)
            return
        }

        rawDiagnosticsFromProject.compute(project.path) { _, previousListIfAny ->
            previousListIfAny?.apply { add(diagnostic) } ?: mutableListOf(diagnostic)
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
    fromProject: Project,
    diagnostic: ToolingDiagnostic,
    key: ToolingDiagnosticId = diagnostic.id,
) {
    report(fromProject, diagnostic, reportOnce = true, ":#$key")
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
internal fun Project.reportDiagnosticImmediately(diagnostic: ToolingDiagnostic) {
    val renderingOptions = ToolingDiagnosticRenderingOptions.forProject(project)
    diagnostic.renderReportedDiagnostic(logger, renderingOptions)
}