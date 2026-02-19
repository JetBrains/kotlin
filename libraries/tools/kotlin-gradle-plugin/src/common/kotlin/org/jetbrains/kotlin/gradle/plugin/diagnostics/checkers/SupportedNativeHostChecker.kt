/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportOncePerGradleProject
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.supportedHosts
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.konan.target.HostManager

internal object SupportedNativeHostChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(
        collector: KotlinToolingDiagnosticsCollector,
    ) {
        // Early returns for non-applicable projects
        val extension = multiplatformExtension ?: return
        val nativeTargets = extension.awaitTargets().withType<KotlinNativeTarget>()
        if (nativeTargets.isEmpty()) return

        if (HostManager.hostIsSupported) return
        collector.reportOncePerGradleProject(
            project,
            KotlinToolingDiagnostics.NativeHostNotSupportedError(
                HostManager.platformName(),
                HostManager().supportedHosts
            )
        )
    }
}
