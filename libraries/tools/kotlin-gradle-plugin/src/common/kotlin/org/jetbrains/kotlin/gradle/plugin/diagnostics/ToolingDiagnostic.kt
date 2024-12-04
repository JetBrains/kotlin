/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi // used in integration tests
data class ToolingDiagnostic(
    val id: String, val message: String, val severity: Severity, val throwable: Throwable? = null,
) {
    enum class Severity {
        /**
         * More visible than most of the output (intuition: yellow-highlighting).
         * Doesn't prevent the build from running.
         *
         * Use for non-critical misconfigurations with low rate of false-positives
         */
        WARNING,

        /**
         * Heavily emphasized in the output (intuition: bold red highlighting).
         *
         * ATTENTION. If a diagnostic with this severity is reported, Kotlin compiler
         * will _not_ be invoked (build will appear failed, as with compilation error)
         *
         * However, Gradle IDE Sync and other tasks that are not connected with
         * any of the Kotlin Compiler and tools (e.g. 'help', 'clean'), will run successfully.
         *
         * Use for critical misconfigurations that need immediate addressing
         */
        ERROR,

        /**
         * Aborts the progress of the current process (Gradle build/Import/...).
         *
         * Please use *extremely* sparingly, as failing the current process can:
         * - mask further errors (forcing users to make multiple runs before fixing all issues)
         *
         * - lead to unpleasant UX in IDE (if the failure happens during import, then depending
         *   on when it happened users might not have even basic IDE assistance, which makes fixing
         *   the root cause very annoying)
         *
         * Use for irreconcilable misconfigurations / malformed input which prevent further
         * configuration _and_ when the graceful degradation (allowing configuration phase to finish)
         * is too expensive.
         */
        FATAL,
    }

    override fun toString(): String {
        return "[$id | $severity] $message"
    }
}
