/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.diagnostics

import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild


internal object GradleCompatibilityCheck {
    internal const val minSupportedGradleVersionString = "7.6.3"

    private val minSupportedGradleVersion = GradleVersion.version(minSupportedGradleVersionString)

    fun Project.runGradleCompatibilityCheck(
        gradleVersionProvider: CurrentGradleVersionProvider = DefaultCurrentGradleVersionProvider
    ) {
        val currentVersion = gradleVersionProvider.get()
        if (gradleVersionProvider.get() < minSupportedGradleVersion) {
            reportDiagnosticOncePerBuild(
                KotlinToolingDiagnostics.IncompatibleGradleVersionTooLowFatalError(
                    currentVersion,
                    minSupportedGradleVersion,
                )
            )
        }
    }

    internal interface CurrentGradleVersionProvider {
        fun get(): GradleVersion
    }

    internal object DefaultCurrentGradleVersionProvider : CurrentGradleVersionProvider {
        override fun get(): GradleVersion = GradleVersion.current()
    }
}