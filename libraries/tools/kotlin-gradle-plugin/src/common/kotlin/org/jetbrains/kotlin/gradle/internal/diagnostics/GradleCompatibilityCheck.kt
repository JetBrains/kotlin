/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.diagnostics

import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinDiagnosticsException
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
            val diagnostic = KotlinToolingDiagnostics.IncompatibleGradleVersionTooLowFatalError(
                currentVersion,
                minSupportedGradleVersion,
            )
            try {
                reportDiagnosticOncePerBuild(diagnostic)
            } catch (e: KotlinDiagnosticsException) {
                throw e
            } catch (e: Throwable) {
                // a special case of diagnostic that may be not reported properly because the build is run on an incompatible Gradle version
                logger.error("Failed to report Gradle version incompatibility diagnostic properly. Throwing it straight away.", e)
                throw KotlinDiagnosticsException(diagnostic.toString())
            }
        }
    }

    internal interface CurrentGradleVersionProvider {
        fun get(): GradleVersion
    }

    internal object DefaultCurrentGradleVersionProvider : CurrentGradleVersionProvider {
        override fun get(): GradleVersion = GradleVersion.current()
    }
}