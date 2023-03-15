/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING

@InternalKotlinGradlePluginApi // used in integration tests
object KotlinToolingDiagnostics {
    object HierarchicalMultiplatformFlagsWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(usedDeprecatedFlags: List<String>) = build(
            "The following properties are obsolete and will be removed in Kotlin 1.9.20:\n" +
                    "${usedDeprecatedFlags.joinToString()}\n" +
                    "Read the details here: https://kotlinlang.org/docs/multiplatform-compatibility-guide.html#deprecate-hmpp-properties",
        )
    }

    object DeprecatedKotlinNativeTargetsDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(usedTargetIds: List<String>) = build(
            "The following deprecated Kotlin/Native targets were used in the project: ${usedTargetIds.joinToString()}"
        )
    }
}
