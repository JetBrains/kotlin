/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.utils.targets

/**
 * Checks if the current subproject supports configuration on demand.
 *
 * See https://docs.gradle.org/current/userguide/configuration_on_demand.html
 */
internal object ConfigurationOnDemandSupportChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val kotlinExtension = project.kotlinExtensionOrNull ?: return

        if (project.gradle.startParameter.isConfigureOnDemand) {
            val unsupportedTargets = kotlinExtension.targets.filter { !it.supportsCod() }

            if (unsupportedTargets.isNotEmpty()) {
                collector.reportOncePerGradleProject(
                    project,
                    KotlinToolingDiagnostics.ConfigurationOnDemandNotSupported(
                        projectDisplayName = project.displayName,
                        namesOfUnsupportedTargets = unsupportedTargets.map { it.name }.toSet(),
                    )
                )
            }
        }
    }

    private fun KotlinTarget.supportsCod(): Boolean {
        return when (platformType) {
            KotlinPlatformType.js,
            KotlinPlatformType.wasm,
                -> false
            KotlinPlatformType.common,
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm,
            KotlinPlatformType.native,
                -> true
        }
    }
}
