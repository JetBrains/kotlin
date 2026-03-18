/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportOncePerGradleBuild
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object DeprecatedNativeHostChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(
        collector: KotlinToolingDiagnosticsCollector,
    ) {
        val extension = multiplatformExtension ?: return
        val nativeTargets = extension.awaitTargets().withType<KotlinNativeTarget>()
        if (nativeTargets.isEmpty()) return

        val host = HostManager.hostOrNull ?: return
        if (host !in KonanTarget.deprecatedTargets) return

        collector.reportOncePerGradleBuild(
            diagnosticsContext,
            KotlinToolingDiagnostics.DeprecatedNativeHostDiagnostic(host.name)
        )
    }
}
