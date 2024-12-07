/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector

internal object InternalGradlePropertiesUsageChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val internalPropertiesUsed = PropertiesProvider.PropertyNames.allInternalProperties().filter { name ->
            kotlinPropertiesProvider.property(name).orNull != null
        }
        if (internalPropertiesUsed.isEmpty()) return

        collector.reportOncePerGradleBuild(
            project,
            KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed(internalPropertiesUsed.sorted()),
            // Known issue in edge case: if several Gradle subprojects declare internal properties, only one will be reported
            key = InternalGradlePropertiesUsageChecker::class.qualifiedName!!
        )
    }
}
