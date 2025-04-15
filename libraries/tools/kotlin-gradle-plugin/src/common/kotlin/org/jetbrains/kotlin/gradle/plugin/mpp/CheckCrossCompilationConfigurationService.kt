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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.BuildServiceUsingKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.CheckCrossCompilationConfigurationService.IncompatibleTargets
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.Serializable

internal abstract class CheckCrossCompilationConfigurationService :
    BuildServiceUsingKotlinToolingDiagnostics<CheckCrossCompilationConfigurationService.Parameters> {

    /**
     * Represents a record of incompatible targets detected during a cross-compilation configuration check.
     *
     * This data class is used to store information about a specific target that has been identified as incompatible
     * with cinterop dependencies when cross-compiling in a Kotlin Multiplatform project. Each record contains the name
     * of the target and a list of cinterop dependencies associated with it.
     *
     * @property target The name of the target that is deemed incompatible.
     * @property cinterops A list of cinterop names that are determined to be incompatible with the target during
     * cross-compilation.
     */
    data class IncompatibleTargets(
        val target: String,
        val cinterops: List<String>,
    ) : Serializable

    interface Parameters : BuildServiceUsingKotlinToolingDiagnostics.Parameters {
        val disableCrossCompilation: Property<Boolean>
        val incompatibleTargets: ListProperty<IncompatibleTargets>
    }

    /**
     * Checks for cross-compilation issues related to Kotlin libraries (klibs) when using cinterops in a multiplatform project.
     *
     * If cross-compilation is not disabled (`kotlin.native.disableKlibsCrossCompilation` is `false`), this method verifies whether the
     * specified targets in the `incompatibleTargets` property contain cinterop dependencies that are incompatible with the
     * host system. If such targets are found, it reports a diagnostic message to help users address this issue.
     */
    fun checkCrossCompilationConfiguration() = with(parameters) {
        if (!disableCrossCompilation.get()) {
            incompatibleTargets.orNull?.forEach { target ->
                reportDiagnostic(
                    KotlinToolingDiagnostics.CrossCompilationWithCinterops(
                        if (HostManager.hostIsMac) WARNING else ERROR,
                        target.target,
                        target.cinterops,
                        HostManager.hostName
                    )
                )
            }
        }
    }
}

internal val Project.kotlinCheckCrossCompilationConfigurationServiceProvider: Provider<CheckCrossCompilationConfigurationService>
    get() = gradle.registerClassLoaderScopedBuildService(CheckCrossCompilationConfigurationService::class) { spec ->
        spec.parameters.setupKotlinToolingDiagnosticsParameters(this)
        spec.parameters.disableCrossCompilation.convention(kotlinPropertiesProvider.disableKlibsCrossCompilation)
        spec.parameters.incompatibleTargets.set(
            multiplatformExtensionOrNull?.let { kotlin ->
                kotlin.targets
                    .withType(KotlinNativeTarget::class.java)
                    .matching { !HostManager().isEnabled(it.konanTarget) }
                    .matching { it.cinterops.isNotEmpty() }
                    .map { target ->
                        IncompatibleTargets(
                            target.targetName,
                            target.cinterops.map { it.name }
                        )
                    }
            }
        )
    }

/**
 * Provides access to the C interop declarations associated with the main compilation of the Kotlin Native target.
 *
 * This property retrieves the `cinterops` configuration from the main compilation, which contains the settings
 * and definitions for interoperating with C libraries in a Kotlin/Native project. It is primarily used internally
 * to configure and manage dependencies on native C libraries.
 */
internal val KotlinNativeTarget.cinterops get() = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops