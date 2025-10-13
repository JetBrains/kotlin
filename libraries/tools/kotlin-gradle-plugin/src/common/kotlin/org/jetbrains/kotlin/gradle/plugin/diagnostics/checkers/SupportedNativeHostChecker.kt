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
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object SupportedNativeHostChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(
        collector: KotlinToolingDiagnosticsCollector,
    ) {
        // Early returns for non-applicable projects
        val extension = multiplatformExtension ?: return
        val nativeTargets = extension.awaitTargets().withType<KotlinNativeTarget>()
        if (nativeTargets.isEmpty()) return

        if (HostManager.hostOrNull != null) return
        collector.reportOncePerGradleProject(
            project,
            KotlinToolingDiagnostics.NativeHostNotSupportedError(
                HostManager.platformName(),
                HostManager().supportedHosts
            )
        )
    }
}

private val KonanTarget.formatedHostName: String
    get() = when (this) {
        KonanTarget.LINUX_X64 -> "Linux (x86_64)"
        KonanTarget.LINUX_ARM64 -> "Linux (aarch64)"
        KonanTarget.MINGW_X64 -> "Windows (x86_64)"
        KonanTarget.MACOS_X64 -> "macOS (x86_64)"
        KonanTarget.MACOS_ARM64 -> "macOS (arm64)"
        // Fallback for any future hosts, though the 'when' should be exhaustive
        else -> visibleName
    }

private val HostManager.supportedHosts: List<String> get() = enabledByHost.keys.map { it.formatedHostName }