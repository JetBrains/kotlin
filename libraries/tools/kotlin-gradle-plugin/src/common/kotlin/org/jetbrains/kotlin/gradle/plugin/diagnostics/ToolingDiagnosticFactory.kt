/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi // used in integration tests
abstract class ToolingDiagnosticFactory(val severity: ToolingDiagnostic.Severity) {
    open val id: String = this::class.simpleName!!

    protected fun build(message: String, throwable: Throwable? = null): ToolingDiagnostic {
        return ToolingDiagnostic(id, message, severity, throwable)
    }

    protected fun String.onlyIf(condition: Boolean) = if (condition) this else ""
}
