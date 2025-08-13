/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject

internal interface UsesCrossCompilationService : Task {
    @get:Internal
    val crossCompilationService: Property<CrossCompilationService?>
}

/**
 * An abstract service responsible for handling cross-compilation-related considerations
 * in Kotlin Multiplatform projects. It provides functionality to verify whether cross-compilation
 * is supported, collect diagnostic information about unsupported configurations, and track native
 * interop usage across projects.
 *
 * This service is designed to assist in ensuring compatibility and compliance with cross-compilation
 * requirements when developing multiplatform projects.
 */
internal abstract class CrossCompilationService : BuildService<CrossCompilationService.Parameter>,
    AutoCloseable {

    @get:Inject
    protected abstract val objectFactory: ObjectFactory

    interface Parameter : BuildServiceParameters {
        val crossCompilationEnabled: Property<Boolean>
        val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>
    }

    companion object {
        private const val LOG_PREFIX = "[CrossCompilationService]"
    }

    private val log = Logging.getLogger(this.javaClass)

    /**
     * An internal, thread-safe map storing discovered cinterops per project path.
     * This is an implementation detail used to calculate [isCrossCompilationSupported] and provide rich diagnostics.
     * Key: Project path (e.g., ":my:project")
     * Value: A set of cinterop names found in that project.
     */
    @Suppress("UNCHECKED_CAST")
    private val cinteropsByProjectPath: MapProperty<String, Set<String>> =
        objectFactory.mapProperty(String::class.java, Set::class.java) as MapProperty<String, Set<String>>

    /**
     * Verifies whether cross-compilation is supported in the current project setup.
     *
     * The method checks if cross-compilation is enabled and ensures there are no CInterop usages
     * in cross-compiled native targets. It also waits for the Kotlin plugin lifecycle stage
     * to reach `AfterFinaliseCompilations` before determining the result.
     *
     * @return `true` if cross-compilation is supported, `false` otherwise.
     */
    internal suspend fun isCrossCompilationSupported(): Boolean {
        val crossCompilationEnabled = parameters.crossCompilationEnabled.get()

        return if (crossCompilationEnabled) {
            KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
            cinteropsByProjectPath.get().values.flatten().isEmpty()
        } else {
            false
        }
    }

    init {
        log.info("$LOG_PREFIX Started. Cross-compilation explicitly enabled: ${parameters.crossCompilationEnabled.get()}")
    }

    /**
     * Scans a single project for cinterop usages in cross-compiled native targets and adds them
     * to the internal [cinteropsByProjectPath] map.
     *
     * This method is intended to be called for each project during the configuration phase.
     *
     * @param project The project to scan.
     */
    internal suspend fun collectCinteropUsagesFrom(project: Project) {
        log.info("$LOG_PREFIX Scanning project '${project.path}' for cinterops on unsupported hosts.")
        val multiplatformExtension = project.multiplatformExtensionOrNull
        if (multiplatformExtension == null || !parameters.crossCompilationEnabled.get()) return

        // Collect all cinterops from targets that are not supported on the current host
        val cinteropsByTarget = multiplatformExtension.awaitTargets()
            .withType(KotlinNativeTarget::class.java)
            .matching { !HostManager().isEnabled(it.konanTarget) }
            .associateWith { target -> target.compilations.flatMap { it.cinterops } }
            .filterValues { it.isNotEmpty() }

        if (cinteropsByTarget.isEmpty()) return

        // Log the findings and collect all cinterop names for the project
        val allCinteropNamesForProject = mutableSetOf<String>()
        cinteropsByTarget.forEach { (target, cinterops) ->
            cinterops.forEach { cinterop ->
                log.info("$LOG_PREFIX Project '${project.path}': Found cinterop '${cinterop.name}' on unsupported host '${target.konanTarget.name}'.")
                allCinteropNamesForProject.add(cinterop.name)
            }
        }

        // Safely update the shared map with the data from this project
        synchronized(cinteropsByProjectPath) {
            val currentFullMap = cinteropsByProjectPath.getOrElse(emptyMap()).toMutableMap()
            currentFullMap[project.path] = allCinteropNamesForProject
            cinteropsByProjectPath.set(currentFullMap)
        }
    }

    override fun close() {
        if (log.isInfoEnabled) {
            val finalCinteropsMap = cinteropsByProjectPath.getOrElse(emptyMap())
            if (finalCinteropsMap.isNotEmpty()) {
                log.warn("$LOG_PREFIX Cross-compilation was disabled due to cinterops on unsupported hosts in the following projects:")
                finalCinteropsMap.forEach { (projectPath, cinterops) ->
                    log.warn("$LOG_PREFIX   - Project '$projectPath': ${cinterops.joinToString()}")
                }
            }
        }
    }
}

internal val Project.crossCompilationServiceProvider: Provider<CrossCompilationService>
    get() = gradle.registerClassLoaderScopedBuildService(CrossCompilationService::class) {
        it.parameters.crossCompilationEnabled.set(kotlinPropertiesProvider.enableKlibsCrossCompilation)
        it.parameters.toolingDiagnosticsCollector.set(kotlinToolingDiagnosticsCollectorProvider)
    }