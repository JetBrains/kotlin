/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.BuildServiceUsingKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.CrossCompilationService.IncompatibleTargets
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

    fun checkKlibsCrossCompilation() = with(parameters) {
        if (!disableKlibsCrossCompilation.get()) {
            incompatibleTargets.orNull?.forEach { target ->
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

internal val Project.kotlinCrossCompilationServiceProvider: Provider<CrossCompilationService>
    get() = gradle.registerClassLoaderScopedBuildService(CrossCompilationService::class) { spec ->
        spec.parameters.setupKotlinToolingDiagnosticsParameters(this)
        spec.parameters.disableKlibsCrossCompilation.convention(kotlinPropertiesProvider.disableKlibsCrossCompilation)
        spec.parameters.incompatibleTargets.set(
            multiplatformExtensionOrNull?.let { kotlin ->
                kotlin.targets
                    .withType(KotlinNativeTarget::class.java)
                    .matching { !it.enabledOnCurrentHostForKlibCompilation }
                    .map { target ->
                        IncompatibleTargets(
                            target.targetName,
                            target.cinterops.map { it.name }
                        )
                    }
            }
        )
    }