/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.BuildServiceUsingKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.XcodeVersionTooHighWarning
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.konan.target.Xcode
import org.jetbrains.kotlin.konan.target.XcodeVersion

internal abstract class XcodeVersionService : BuildServiceUsingKotlinToolingDiagnostics<XcodeVersionService.Parameters> {

    interface Parameters : BuildServiceUsingKotlinToolingDiagnostics.Parameters {
        val ignoreVersionCompatibilityCheck: Property<Boolean>
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<XcodeVersionService> {
            return project.gradle.registerClassLoaderScopedBuildService(XcodeVersionService::class) { spec ->
                spec.parameters.setupKotlinToolingDiagnosticsParameters(project)
                spec.parameters.ignoreVersionCompatibilityCheck.convention(project.kotlinPropertiesProvider.appleIgnoreXcodeVersionCompatibility)
            }
        }
    }

    private val logger = Logging.getLogger(this::class.java)

    val version: XcodeVersion by lazy {
        Xcode.findCurrent().version.also(::checkVersionCompatibility)
    }

    private fun checkVersionCompatibility(xcodeVersion: XcodeVersion) = with(parameters) {
        if (!ignoreVersionCompatibilityCheck.get() && xcodeVersion > XcodeVersion.maxTested) {
            toolingDiagnosticsCollector.get().report(
                this, logger,
                XcodeVersionTooHighWarning(
                    xcodeVersionString = xcodeVersion.toString(),
                    maxTested = XcodeVersion.maxTested.toString(),
                )
            )
        }
    }
}
