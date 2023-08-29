/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private typealias ToolingDiagnosticId = String

internal abstract class KotlinToolingDiagnosticsCollector : BuildService<BuildServiceParameters.None> {
    /**
     * When collector is in transparent mode, any diagnostics received will be immediately rendered
     * instead of collected
     */
    @Volatile
    private var isTransparent: Boolean = false

    private val rawDiagnosticsFromProject: MutableMap<ToolingDiagnostic.Location, MutableList<ToolingDiagnostic>> = ConcurrentHashMap()
    private val reportedIds: MutableSet<ToolingDiagnosticId> = Collections.newSetFromMap(ConcurrentHashMap())

   fun getDiagnosticsForLocation(location: ToolingDiagnostic.Location): Collection<ToolingDiagnostic> =
        rawDiagnosticsFromProject[location] ?: emptyList()

    fun report(project: Project, diagnostic: ToolingDiagnostic) {
        val location = project.toLocation()
        diagnostic.attachLocation(location)
        handleDiagnostic(diagnostic, location, ToolingDiagnosticRenderingOptions.forProject(project), project.logger)
    }

    fun report(task: UsesKotlinToolingDiagnostics, diagnostic: ToolingDiagnostic) {
        val location = task.toLocation()
        diagnostic.attachLocation(location)
        val options = task.diagnosticRenderingOptions.get()
        handleDiagnostic(diagnostic, location, options, task.logger)
    }

    fun reportOncePerGradleBuild(fromProject: Project, diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.factoryId) {
        val location = fromProject.toLocation()
        diagnostic.attachLocation(location)
        handleDiagnostic(
            diagnostic,
            location,
            ToolingDiagnosticRenderingOptions.forProject(fromProject),
            fromProject.logger,
            deduplicationKey = key
        )
    }

    fun switchToTransparentMode() {
        isTransparent = true
    }

    private fun handleDiagnostic(
        diagnostic: ToolingDiagnostic,
        location: ToolingDiagnostic.Location,
        options: ToolingDiagnosticRenderingOptions,
        logger: Logger,
        deduplicationKey: String? = null
    ) {
        // 1. Check suppression or duplicated reporting. Reporting a suppressed or duplicated diagnostic shouldn't cause any side-effects,
        // so we're returning right away if it is suppressed
        if (diagnostic.isSuppressed(options) || deduplicationKey != null && !reportedIds.add(deduplicationKey)) return

        // 2. Store diagnostic. Note that we don't care about any external user-visible effects this diagnostic causes.
        // As a specific consequence, stored diagnostics can be FATAL. This shouldn't make any difference on production, but is convenient
        // for tests
        rawDiagnosticsFromProject.compute(location) { _, previousListIfAny ->
            previousListIfAny?.apply { add(diagnostic) } ?: mutableListOf(diagnostic)
        }

        // 3. Produce user-visible effects if necessary
        when {
            diagnostic.severity == ToolingDiagnostic.Severity.FATAL -> throw diagnostic.createAnExceptionForFatalDiagnostic(options)

            isTransparent -> renderReportedDiagnostic(diagnostic, logger, options)
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

internal fun Project.reportDiagnosticOncePerBuild(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.factoryId) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(this, diagnostic, key)
}
