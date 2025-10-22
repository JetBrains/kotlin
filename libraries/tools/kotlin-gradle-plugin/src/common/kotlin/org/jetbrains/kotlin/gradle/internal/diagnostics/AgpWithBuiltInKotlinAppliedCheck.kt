/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.diagnostics

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinAndroidExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerProject
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import java.util.concurrent.atomic.AtomicBoolean

internal object AgpWithBuiltInKotlinAppliedCheck {
    val minimalBuiltInKotlinSupportedAgpVersion = AndroidGradlePluginVersion(9, 0, 0, "alpha01")

    fun Project.runAgpWithBuiltInKotlinIfAppliedCheck(
        agpVersionProvider: AndroidGradlePluginVersionProvider = AndroidGradlePluginVersionProvider.Default
    ) {
        val isKotlinAndroidExtensionExists = kotlinAndroidExtensionOrNull != null
        val agpVersion = agpVersionProvider.get()
        if (isKotlinAndroidExtensionExists &&
            agpVersion != null &&
            agpVersion >= minimalBuiltInKotlinSupportedAgpVersion
        ) {
            project.reportDiagnostic(
                KotlinToolingDiagnostics.AgpWithBuiltInKotlinIsAlreadyApplied(
                    project.buildFile.relativeTo(project.rootDir),
                    Throwable()
                )
            )
        }
        // it's important to have strict ordering between those 2 checks.
        // IncompatibleWithTheNewAgpDsl must not hide AgpWithBuiltInKotlinIsAlreadyApplied
        checkIfNewDslIsUsed(agpVersionProvider)
    }

    private fun Project.checkIfNewDslIsUsed(agpVersionProvider: AndroidGradlePluginVersionProvider) {
        val wasChecked = AtomicBoolean(false)
        androidPluginIds.forEach { agpPluginId ->
            plugins.withId(agpPluginId) {
                if (!wasChecked.getAndSet(true)) {
                    try {
                        project.extensions.getByName("android") as BaseExtension
                        val androidVersion = agpVersionProvider.get()
                        if (androidVersion != null && androidVersion >= minimalBuiltInKotlinSupportedAgpVersion) {
                            // Report deprecation with AGP 9.+ when Android deprecated Variants API is enabled
                            // and built-in Kotlin support is disabled. Other cases related to AGP 9.+ change are covered
                            // by other diagnostics like 'KotlinToolingDiagnostics.IncompatibleWithTheNewAgpDsl' or
                            // 'KotlinToolingDiagnostics.AgpWithBuiltInKotlinIsAlreadyApplied'.
                            // This diagnostic should be fired at the end of related diagnostics checks and only if the checks are passed.
                            project.reportDiagnosticOncePerProject(
                                KotlinToolingDiagnostics.DeprecatedKotlinAndroidPlugin(path)
                            )
                        }
                    } catch (e: ClassCastException) {
                        project.reportDiagnostic(
                            KotlinToolingDiagnostics.IncompatibleWithTheNewAgpDsl(e)
                        )
                    }
                }
            }
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
