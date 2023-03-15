/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi // used in integration tests
abstract class ToolingDiagnosticFactory(private val predefinedSeverity: ToolingDiagnostic.Severity?, customId: String?) {
    constructor(customId: String) : this(null, customId)
    constructor(predefinedSeverity: ToolingDiagnostic.Severity?) : this(predefinedSeverity, null)

    open val id: String = customId ?: this::class.simpleName!!

    protected fun build(message: String, severity: ToolingDiagnostic.Severity? = null): ToolingDiagnostic {
        if (severity == null && predefinedSeverity == null) {
            error(
                "Can't determine severity. " +
                        "Either provide it in constructor of ToolingDiagnosticFactory, or in the 'build'-function invocation"
            )
        }
        if (severity != null && predefinedSeverity != null) {
            error(
                "Please provide severity either in ToolingDiagnosticFactory constructor, or as the 'build'-function parameter," +
                        " but not both at once"
            )
        }
        return ToolingDiagnostic(id, message, severity ?: predefinedSeverity!!)
    }

    protected fun String.onlyIf(condition: Boolean) = if (condition) this else ""
}
