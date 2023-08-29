/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.io.Serializable

private typealias ToolingDiagnosticId = String

internal abstract class KotlinToolingDiagnosticsCollector : BuildService<KotlinToolingDiagnosticsCollector.Parameters> {

    abstract class Parameters : BuildServiceParameters, Serializable {
        /**
         * When collector is in transparent mode, any diagnostics received will be immediately rendered
         * instead of collected
         */
        abstract val transparent: Property<Boolean>

        abstract val rawDiagnosticsFromProject: MapProperty<ToolingDiagnostic.Location, MutableList<ToolingDiagnostic>>
        abstract val reportedIds: SetProperty<ToolingDiagnosticId>

        fun copyStateFrom(other: Parameters) {
            this.rawDiagnosticsFromProject.putAll(
                // .map { it } is a workaround for a weird Gradle exception/bug:
                //
                // java.lang.IllegalArgumentException: Cannot set the value of a property of type java.util.Map with key type
                // org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic$Location and value type java.util.List using a
                // provider with key type java.lang.Object and value type java.lang.Object.
                other.rawDiagnosticsFromProject.map { it }
            )
            this.reportedIds.addAll(other.reportedIds)
            this.transparent.set(other.transparent)
        }
    }

    fun getDiagnosticsForLocation(location: ToolingDiagnostic.Location): Collection<ToolingDiagnostic> =
        parameters.rawDiagnosticsFromProject.get()[location] ?: emptyList()

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
        parameters.transparent.set(true)
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
        if (diagnostic.isSuppressed(options) || deduplicationKey != null && parameters.reportedIds.get().contains(deduplicationKey)) return

        // 2. Store diagnostic. Note that we don't care about any external user-visible effects this diagnostic causes.
        // As a specific consequence, stored diagnostics can be FATAL. This shouldn't make any difference on production, but is convenient
        // for tests
        if (deduplicationKey != null) parameters.reportedIds.add(deduplicationKey)

        val updatedValue = parameters.rawDiagnosticsFromProject
            .getting(location)
            .getOrElse(mutableListOf())
            .apply { add(diagnostic) }
        parameters.rawDiagnosticsFromProject.put(location, updatedValue)

        // 3. Produce user-visible effects if necessary
        when {
            diagnostic.severity == ToolingDiagnostic.Severity.FATAL -> throw diagnostic.createAnExceptionForFatalDiagnostic(options)

            parameters.transparent.get() -> renderReportedDiagnostic(diagnostic, logger, options)
        }
    }
}

internal val Project.kotlinToolingDiagnosticsCollectorForConfiguration: Provider<KotlinToolingDiagnosticsCollector>
    get() = gradle.registerClassLoaderScopedBuildService(KotlinToolingDiagnosticsCollector::class) {
        it.parameters.apply {
            reportedIds.set(emptySet())
            rawDiagnosticsFromProject.set(emptyMap())
            transparent.set(false)
        }
    }

internal val Project.kotlinToolingDiagnosticsCollectorForExecution: Provider<KotlinToolingDiagnosticsCollector>
    get() {
        val kClass = KotlinToolingDiagnosticsCollector::class
        return gradle.sharedServices.registerIfAbsent("${kClass.simpleName}_EXECUTION_${kClass.java.classLoader.hashCode()}", kClass.java) {
            it.parameters.copyStateFrom(kotlinToolingDiagnosticsCollector.parameters)
        }
    }

internal val Project.kotlinToolingDiagnosticsCollector: KotlinToolingDiagnosticsCollector
    get() = kotlinToolingDiagnosticsCollectorForConfiguration.get()

internal fun Project.reportDiagnostic(diagnostic: ToolingDiagnostic) {
    kotlinToolingDiagnosticsCollector.report(this, diagnostic)
}

internal fun Project.reportDiagnosticOncePerBuild(diagnostic: ToolingDiagnostic, key: ToolingDiagnosticId = diagnostic.factoryId) {
    kotlinToolingDiagnosticsCollector.reportOncePerGradleBuild(this, diagnostic, key)
}
