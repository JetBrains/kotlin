/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerProject
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import java.util.concurrent.atomic.AtomicBoolean

internal object AgpCompatibilityCheck {
    val minimalSupportedAgpVersion = AndroidGradlePluginVersion(7, 3, 1)

    fun Project.runAgpCompatibilityCheck(
        agpVersionProvider: AndroidGradlePluginVersionProvider = AndroidGradlePluginVersionProvider.Default
    ) {
        val wasChecked = AtomicBoolean(false)
        androidPluginIds.forEach { agpPluginId ->
            plugins.withId(agpPluginId) {
                if (!wasChecked.getAndSet(true)) {
                    checkAgpVersion(agpVersionProvider)
                }
            }
        }
    }

    private fun Project.checkAgpVersion(
        agpVersionProvider: AndroidGradlePluginVersionProvider,
    ) {
        val androidGradlePluginVersion = agpVersionProvider.get()

        if (androidGradlePluginVersion == null) {
            // TODO: pass plugin id to diagnostic
            reportDiagnosticOncePerProject(KotlinToolingDiagnostics.FailedToGetAgpVersionWarning())
            return
        }

        if (androidGradlePluginVersion < minimalSupportedAgpVersion) {
            reportDiagnosticOncePerProject(
                KotlinToolingDiagnostics.IncompatibleAgpVersionTooLowFatalError(
                    androidGradlePluginVersion.toString(),
                    minimalSupportedAgpVersion.toString(),
                )
            )
        }
    }

    interface AndroidGradlePluginVersionProvider {
        fun get(): AndroidGradlePluginVersion?

        object Default : AndroidGradlePluginVersionProvider {
            override fun get(): AndroidGradlePluginVersion? {
                return AndroidGradlePluginVersion.currentOrNull
            }
        }
    }
}