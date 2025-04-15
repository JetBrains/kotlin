/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.BuildServiceUsingKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.Serializable

internal abstract class CrossCompilationService : BuildServiceUsingKotlinToolingDiagnostics<CrossCompilationService.Parameters> {

    data class IncompatibleTargets(
        val target: String,
        val cinterops: List<String>,
    ) : Serializable

    interface Parameters : BuildServiceUsingKotlinToolingDiagnostics.Parameters {
        val disableKlibsCrossCompilation: Property<Boolean>
        val incompatibleTargets: ListProperty<IncompatibleTargets>
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<CrossCompilationService> {
            return project.gradle.registerClassLoaderScopedBuildService(CrossCompilationService::class) { spec ->
                spec.parameters.setupKotlinToolingDiagnosticsParameters(project)
                spec.parameters.disableKlibsCrossCompilation.convention(project.kotlinPropertiesProvider.disableKlibsCrossCompilation)
                spec.parameters.incompatibleTargets.convention(
                    project.multiplatformExtension.targets
                        .withType(KotlinNativeTarget::class.java)
                        .matching { !HostManager().isEnabled(it.konanTarget) }
                        .map { target ->
                            IncompatibleTargets(
                                target.targetName,
                                target.cinterops.map { it.name }
                            )
                        }
                )
            }
        }
    }

    fun checkKlibsCrossCompilation() = with(parameters) {
        if (!disableKlibsCrossCompilation.get()) {
            incompatibleTargets.get().forEach { target ->
                reportDiagnostic(
                    KotlinToolingDiagnostics.CrossCompilationWithCinterops(
                        target.target,
                        target.cinterops,
                        HostManager.hostName
                    )
                )
            }
        }
    }
}