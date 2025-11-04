/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.toKotlinVersion

internal object DisabledNativeCacheChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        if (multiplatformExtension == null) return
        KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
        val targets = multiplatformExtension.awaitTargets().withType<KotlinNativeTarget>()
        if (targets.isEmpty()) return
        val nativeVersion = project.nativeProperties.kotlinNativeVersion.map {
            KotlinToolingVersion(it).toKotlinVersion()
        }.orNull

        targets.configureEach { target ->
            target.binaries.matching { it.disableCacheSettings.isNotEmpty() }.configureEach { binary ->
                binary.disableCacheSettings.forEach { disableCache ->
                    collector.report(
                        project,
                        KotlinToolingDiagnostics.NativeCacheDisabledDiagnostic(
                            nativeVersion,
                            binary.konanTarget,
                            disableCache.reason,
                            disableCache.issueUrl
                        )
                    )
                }
            }
        }
    }
}