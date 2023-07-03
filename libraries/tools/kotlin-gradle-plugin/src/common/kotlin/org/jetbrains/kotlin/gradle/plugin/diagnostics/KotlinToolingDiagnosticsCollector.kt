/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
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
        val rawDiagnostics = rawDiagnosticsFromProject[project.path] ?: return emptyList()
        val options = ToolingDiagnosticRenderingOptions.forProject(project)
        return rawDiagnostics.withoutSuppressed(options)
    }

    fun report(project: Project, diagnostic: ToolingDiagnostic) {
        saveDiagnostic(project, diagnostic)
    }

    fun report(task: UsesKotlinToolingDiagnostics, diagnostic: ToolingDiagnostic) {
        val options = task.diagnosticRenderingOptions.get()
        if (!diagnostic.isSuppressed(options)) {
            renderReportedDiagnostic(diagnostic, task.logger, options.isVerbose)
        }
    }

    fun reportOncePerGradleProject(fromProject: Project, diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
        if (reportedIds.add("${fromProject.path}#$key")) {
            saveDiagnostic(fromProject, diagnostic)
        }
    }

    fun reportOncePerGradleBuild(fromProject: Project, diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.id) {
        if (reportedIds.add(":#$key")) {
            saveDiagnostic(fromProject, diagnostic)
        }
    }

    fun switchToTransparentMode() {
        isTransparent = true
    }

    private fun saveDiagnostic(project: Project, diagnostic: ToolingDiagnostic) {
        if (isTransparent) {
            renderReportedDiagnostic(diagnostic, project.logger, project.kotlinPropertiesProvider.internalVerboseDiagnostics)
            return
        }

        rawDiagnosticsFromProject.compute(project.path) { _, previousListIfAny ->
            previousListIfAny?.apply { add(diagnostic) } ?: mutableListOf(diagnostic)
        }

        if (diagnostic.severity == ToolingDiagnostic.Severity.FATAL) {
            if (diagnostic.throwable != null)
                throw InvalidUserCodeException(diagnostic.message, diagnostic.throwable)
            else throw InvalidUserCodeException(diagnostic.message)
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
