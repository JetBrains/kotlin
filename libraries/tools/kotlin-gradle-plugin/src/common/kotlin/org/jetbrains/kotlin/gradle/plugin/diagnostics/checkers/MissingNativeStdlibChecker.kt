/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.konanDistribution

internal object MissingNativeStdlibChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val targets = multiplatformExtension?.awaitTargets() ?: return
        if (targets.isEmpty() || // misconfigured project
            targets.none { it is KotlinNativeTarget } || // no K/N targets
            project.hasProperty("kotlin.native.nostdlib") || // suppressed
            project.konanDistribution.stdlib.exists()
        ) return

        collector.report(project, KotlinToolingDiagnostics.NativeStdlibIsMissingDiagnostic(
            PropertiesProvider.KOTLIN_NATIVE_HOME.takeIf { kotlinPropertiesProvider.nativeHome != null }
        ))
    }
}
