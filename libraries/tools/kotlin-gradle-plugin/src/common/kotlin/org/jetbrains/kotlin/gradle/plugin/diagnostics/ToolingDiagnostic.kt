/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi // used in integration tests
data class ToolingDiagnostic(val id: String, val message: String, val severity: Severity) {
    enum class Severity {
        WARNING,

        /**
         * Stronger highlighting than WARNING, but doesn't prevent further actions (e.g. further
         * tasks) from being executed
         */
        ERROR,

        /**
         * Aborts the progress of the current process (Gradle build/Import/...).
         *
         * Please use *extremely* sparingly, as failing the current process can:
         * - mask further errors (forcing users to make multiple runs before fixing all issues)
         *
         * - lead to unpleasant UX in IDE (if the failure happens during import, then depending
         *   on when it happened users might not have even basic IDE assistance, which makes it
         *   hard to fix the issue)
         */
        FATAL,
    }

    override fun toString(): String {
        return "[$id | $severity] $message"
    }
}
