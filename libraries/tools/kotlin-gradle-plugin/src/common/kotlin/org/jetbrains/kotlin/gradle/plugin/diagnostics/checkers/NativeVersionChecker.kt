/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal object NativeVersionChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val nativeVersion = project.nativeProperties.kotlinNativeVersion.map { KotlinToolingVersion(it) }.orNull
        val kotlinVersion = project.kotlinToolingVersion
        if (nativeVersion != null && nativeVersion > kotlinVersion) {
            collector.report(project, KotlinToolingDiagnostics.NewNativeVersionDiagnostic(nativeVersion, kotlinVersion))
        }
        if (nativeVersion != null && nativeVersion < kotlinVersion) {
            collector.report(project, KotlinToolingDiagnostics.OldNativeVersionDiagnostic(nativeVersion, kotlinVersion))
        }
    }
}