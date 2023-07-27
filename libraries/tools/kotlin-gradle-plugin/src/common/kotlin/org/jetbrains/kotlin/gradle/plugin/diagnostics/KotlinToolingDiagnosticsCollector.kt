/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private typealias ToolingDiagnosticId = String
private typealias GradleProjectPath = String

internal abstract class KotlinToolingDiagnosticsCollector : BuildService<BuildServiceParameters.None> {
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

    fun report(project: Project, diagnostic: ToolingDiagnostic) {
        handleDiagnostic(project, diagnostic)
    }

    fun report(task: UsesKotlinToolingDiagnostics, diagnostic: ToolingDiagnostic) {
        val options = task.diagnosticRenderingOptions.get()
        if (!diagnostic.isSuppressed(options)) {
            renderReportedDiagnostic(diagnostic, task.logger, options)
        }
    }

    fun reportOncePerGradleProject(fromProject: Project, diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
        if (reportedIds.add("${fromProject.path}#$key")) {
            handleDiagnostic(fromProject, diagnostic)
        }
    }

    fun reportOncePerGradleBuild(fromProject: Project, diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
        if (reportedIds.add(":#$key")) {
            handleDiagnostic(fromProject, diagnostic)
        }
    }

    fun switchToTransparentMode() {
        isTransparent = true
    }

    private fun handleDiagnostic(project: Project, diagnostic: ToolingDiagnostic) {
        val options = ToolingDiagnosticRenderingOptions.forProject(project)
        if (diagnostic.isSuppressed(options)) return

        if (isTransparent) {
            renderReportedDiagnostic(diagnostic, project.logger, options)
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
    get() = gradle.registerClassLoaderScopedBuildService(KotlinToolingDiagnosticsCollector::class)


internal val Project.kotlinToolingDiagnosticsCollector: KotlinToolingDiagnosticsCollector
    get() = kotlinToolingDiagnosticsCollectorProvider.get()

internal fun Project.reportDiagnostic(diagnostic: ToolingDiagnostic) {
    kotlinToolingDiagnosticsCollector.report(this, diagnostic)
}

internal fun Project.reportDiagnosticOncePerProject(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleProject(this, diagnostic, key)
}

internal fun Project.reportDiagnosticOncePerBuild(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(this, diagnostic, key)
}
