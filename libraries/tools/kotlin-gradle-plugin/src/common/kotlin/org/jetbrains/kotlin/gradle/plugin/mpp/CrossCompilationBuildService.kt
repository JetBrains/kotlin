/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.target.HostManager
import java.util.concurrent.ConcurrentHashMap

internal interface UsesCrossCompilationService : Task {
    @get:Internal
    val crossCompilationService: Property<CrossCompilationService?>
}

internal abstract class CrossCompilationService : BuildService<CrossCompilationService.Parameter>,
    AutoCloseable,
    OperationCompletionListener {
    interface Parameter : BuildServiceParameters {
        val crossCompilationEnabled: Property<Boolean>
        val toolingDiagnosticsCollector: Property<KotlinToolingDiagnosticsCollector>
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<CrossCompilationService> =
            project.gradle.registerClassLoaderScopedBuildService(CrossCompilationService::class) {
                it.parameters.crossCompilationEnabled.set(project.kotlinPropertiesProvider.enableKlibsCrossCompilation)
                it.parameters.toolingDiagnosticsCollector.set(project.kotlinToolingDiagnosticsCollectorProvider)
            }.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesCrossCompilationService::class.java.name) {
                    project.tasks.withType<UsesCrossCompilationService>().configureEach { task ->
                        task.crossCompilationService.value(serviceProvider).disallowChanges()
                        task.usesService(serviceProvider)
                    }
                }
            }
    }

    private val log = Logging.getLogger(this.javaClass)

    // A thread-safe map to store cinterop names found in each project.
    // The key is the project path, and the value is a set of cinterop names.
    private val cinteropsByProject = ConcurrentHashMap<String, MutableSet<String>>()

    init {
        log.info("Cross compilation service is started, state: ${parameters.crossCompilationEnabled.get()}")
    }

    internal suspend fun detectCinteropUsagesInMultipleProjects(project: Project, kotlinPluginVersion: String) {
        log.info("Check for cinterops in project: ${project.name}, version: $kotlinPluginVersion")
        val multiplatformExtension = project.multiplatformExtensionOrNull
        log.info("Multiplatform extension: $multiplatformExtension")
        log.info("crossCompilationEnabled: ${parameters.crossCompilationEnabled.get()}")
        if (multiplatformExtension == null || parameters.crossCompilationEnabled.get().not()) return

        multiplatformExtension.awaitTargets()
            .withType(KotlinNativeTarget::class.java)
            // Filter for targets that are not enabled on the current host (i.e., they are cross-compiled)
            .matching { !HostManager().isEnabled(it.konanTarget) }
            .configureEach { target ->
                target.compilations.configureEach { compilation ->
                    compilation.cinterops.configureEach { cinterop ->
                        // Collect the cinterop name for the current project.
                        // We use computeIfAbsent to safely initialize the set for a new project.
                        val projectCinterops = cinteropsByProject.computeIfAbsent(project.path) {
                            ConcurrentHashMap.newKeySet() // Use a thread-safe set
                        }
                        projectCinterops.add(cinterop.name)
                        log.info("Cinterop ${cinterop.name} is used in project ${project.path}")
                    }
                }
            }
    }

    override fun close() {
        log.info("Cross compilation service is closed")
    }

    override fun onFinish(event: FinishEvent?) {
        log.info("Cross compilation onFinish: $event")
    }
}