/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild

internal fun Project.reportDisabledNativeTargetTaskWarningIfNeeded(
    isEnabledOnCurrentHost: Boolean,
    taskName: String,
    targetName: String,
    currentHost: String,
    reason: String,
) {
    if (isEnabledOnCurrentHost || kotlinPropertiesProvider.ignoreDisabledNativeTargets == true) return

    reportDiagnosticOncePerBuild(
        KotlinToolingDiagnostics.DisabledNativeTargetTaskWarning(
            taskName = taskName,
            targetName = targetName,
            currentHost = currentHost,
            reason = reason,
        )
    )
}
