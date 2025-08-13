/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of a source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable
import javax.inject.Inject

internal interface UsesCrossCompilationService : Task {
    @get:Internal
    val crossCompilationService: Property<CrossCompilationService>
}

/**
 * Service for managing cross-compilation capabilities in multi-platform Kotlin projects.
 *
 * This service analyzes and manages cross-compilation conditions for Kotlin/Native targets.
 * It determines if cross-compilation is supported for a specific target by checking two conditions:
 * 1. Cross-compilation must be enabled via the `[PropertiesProvider.PropertyNames.KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION]` property.
 * 2. The specific target must not have any cinterop dependencies when being compiled on an unsupported host.
 *
 * This service uses a `CompletableFuture` to coordinate between configuration coroutines
 * that collect cinterop data and other coroutines that need to wait for this data
 * before proceeding (e.g., to create publishing tasks).
 */
internal abstract class CrossCompilationService : BuildService<CrossCompilationService.Parameter>,
    AutoCloseable {

    @get:Inject
    protected abstract val objectFactory: ObjectFactory

    interface Parameter : BuildServiceParameters {
        val crossCompilationEnabled: Property<Boolean>
        val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>
    }

    /**
     * Internal data class to model the mapping between a Kotlin/Native target and its cinterop libraries.
     */
    private data class CInteropUsage(
        val target: KonanTarget,
        val cinterops: Set<String>,
    ) : Serializable {
        override fun toString() = "CInteropUsage(target=${target.name}, cinterops=$cinterops)"
    }

    companion object {
        private const val LOG_PREFIX = "[CrossCompilationService]"

        fun registerIfAbsent(project: Project): Provider<CrossCompilationService> =
            project.gradle.registerClassLoaderScopedBuildService(CrossCompilationService::class) {
                it.parameters.crossCompilationEnabled.set(project.kotlinPropertiesProvider.enableKlibsCrossCompilation)
                it.parameters.toolingDiagnosticsCollector.set(project.kotlinToolingDiagnosticsCollectorProvider)
            }.also { serviceProvider ->
                // Wire the service to tasks that use it. This is safe for project isolation.
                SingleActionPerProject.run(project, UsesCrossCompilationService::class.java.name) {
                    project.tasks.withType<UsesCrossCompilationService>().configureEach { task ->
                        task.crossCompilationService.value(serviceProvider).disallowChanges()
                        task.usesService(serviceProvider)
                    }
                }
            }
    }

    private val hostManager by lazy { HostManager() }
    private val log = Logging.getLogger(this.javaClass)

    /**
     * A raw, temporary map for collecting cinterop usages during the configuration phase.
     * This map is processed into `finalizedCinteropTargets` for efficient lookups later.
     */
    @Suppress("UNCHECKED_CAST")
    private val cinteropsByProjectPath: MapProperty<String, Set<CInteropUsage>> =
        objectFactory.mapProperty(String::class.java, Set::class.java) as MapProperty<String, Set<CInteropUsage>>

    /**
     * A future that completes when the cinterop data has been finalized.
     * Suspending functions can `await()` this to safely access the finalized data.
     */
    private val finalizationComplete = CompletableFuture<Unit>()

    /**
     * The final, efficient lookup set. It contains all [KonanTarget]s that have cinterops
     * on an unsupported host, making the final support check a simple `Set.contains()` call.
     */
    @Volatile
    private lateinit var finalizedCinteropTargets: Set<KonanTarget>

    /**
     * A dedicated lock object for the synchronized block in `finalizeCinteropData`.
     */
    private val finalizationLock = Any()

    init {
        log.info("$LOG_PREFIX Started. Cross-compilation enabled: ${parameters.crossCompilationEnabled.get()}")
    }

    /**
     * Determines if cross-compilation is supported for a given target platform.
     * This is a `suspend` function designed to be called from a configuration coroutine.
     *
     * It first awaits the finalization of all cinterop data from the build, then performs
     * an efficient check to see if the given target is affected by any cinterop restrictions.
     *
     * @param target The target platform to check.
     * @return `true` if cross-compilation is supported for the given target, otherwise `false`.
     */
    internal suspend fun isCrossCompilationSupported(target: KonanTarget): Boolean {
        // Case 1: The host is not supported
        if (HostManager.hostOrNull == null) return false

        // Case 2: The host can build this target natively. Always supported.
        if (hostManager.isEnabled(target)) {
            return true
        }

        // Case 3: It's only possible if the global flag is enabled.
        if (!parameters.crossCompilationEnabled.get()) {
            return false
        }

        // Case 4: Cross-compilation is enabled. Now we must check for cinterops.
        // Wait for the finalization of cinterop data from all projects.
        finalizationComplete.await()

        // Supported only if this specific target is NOT in the set of targets with cinterops.
        return target !in finalizedCinteropTargets
    }

    /**
     * Scans a project for cinterop usages on unsupported Kotlin/Native targets and collects them.
     * This method is called for each project during the configuration phase.
     */
    internal suspend fun collectCinteropUsagesFrom(project: Project) {
        log.info("$LOG_PREFIX Scanning project '${project.path}' for cinterops on unsupported hosts.")
        val multiplatformExtension = project.multiplatformExtensionOrNull ?: return

        val cinteropsByTarget = multiplatformExtension.awaitTargets()
            .withType(KotlinNativeTarget::class.java)
            .matching { !hostManager.isEnabled(it.konanTarget) }
            .associateWith { target -> target.compilations.flatMap { it.cinterops } }
            .filterValues { it.isNotEmpty() }

        if (cinteropsByTarget.isEmpty()) return

        val cinteropUsages = cinteropsByTarget.map { (target, cinterops) ->
            val cinteropNames = cinterops.map { it.name }
            log.info(
                "$LOG_PREFIX Project '${project.path}': Found cinterops '${cinteropNames.joinToString()}' " +
                        "on unsupported host '${target.konanTarget.name}'."
            )
            CInteropUsage(target.konanTarget, cinteropNames.toSet())
        }

        // This synchronized block is necessary to safely update the map from concurrent project configurations.
        synchronized(cinteropsByProjectPath) {
            val currentFullMap = cinteropsByProjectPath.getOrElse(emptyMap()).toMutableMap()
            currentFullMap[project.path] = cinteropUsages.toSet()
            cinteropsByProjectPath.set(currentFullMap)
        }
    }

    /**
     * Processes the raw collected cinterop data into the final, efficient lookup set.
     * This is called only once for the entire build, after all projects have been evaluated.
     *
     * This method is synchronized and idempotent to be safe for concurrent execution.
     */
    internal fun finalizeCinteropData() {
        // Use a dedicated lock object to safely perform the check-and-set operation.
        synchronized(finalizationLock) {
            // Check if already finalized.
            if (this::finalizedCinteropTargets.isInitialized) return

            finalizedCinteropTargets = cinteropsByProjectPath.getOrElse(emptyMap())
                .values
                .flatten()
                .map { it.target }
                .toSet()

            // Complete the future to unblock any waiting coroutines.
            finalizationComplete.complete(Unit)
        }
    }


    override fun close() {
        if (log.isInfoEnabled) {
            val finalCinteropsMap = cinteropsByProjectPath.getOrElse(emptyMap())
            if (finalCinteropsMap.isNotEmpty()) {
                log.warn("$LOG_PREFIX Cross-compilation might be disabled for some targets due to cinterops on unsupported hosts:")
                finalCinteropsMap.forEach { (projectPath, cinterops) ->
                    log.warn("$LOG_PREFIX   - Project '$projectPath': ${cinterops.joinToString()}")
                }
            }
        }
    }
}

internal val Project.crossCompilationServiceProvider: Provider<CrossCompilationService>
    get() = CrossCompilationService.registerIfAbsent(this)