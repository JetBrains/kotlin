/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

/**
 * This class is made for checking if the required parameters for the CInteropProcess task are specified in the Gradle project.
 */
internal object CInteropInputChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        // We use "Ready for Execution" stage to ensure that any task parameters are finalized prior to execution.
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        project.tasks.withType(CInteropProcess::class.java).configureEach {
            if (!it.definitionFile.isPresent && it.packageName.isNullOrBlank()) {
                project.reportDiagnostic(KotlinToolingDiagnostics.CInteropRequiredParametersNotSpecifiedError())
            }
        }
    }
}