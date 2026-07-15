/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

/**
 * Represents a Kotlin tooling (Kotlin Gradle plugin) diagnotics severity.
 *
 * @since 2.4.20
 */
enum class KotlinToolingDiagnosticsSeverity {

    /**
     * Used for non-critical misconfigurations with low rate of false-positives.
     *
     * More visible than most of the output (intuition: yellow-highlighting).
     * Doesn't prevent the build from running.
     *
     * @since 2.4.20
     */
    WARNING,

    /**
     * Used for critical misconfigurations that need immediate addressing.
     *
     * Heavily emphasized in the output (intuition: bold red highlighting).
     *
     * **ATTENTION**: If a diagnostic with this severity is reported, Kotlin compiler
     * will _not_ be invoked (build will appear failed, as with compilation error).
     *
     * However, Gradle IDE Sync and other tasks that are not connected with
     * any of the Kotlin Compiler and tools (e.g. 'help', 'clean'), will run successfully.
     *
     * @since 2.4.20
     */
    ERROR,

    /**
     * Same display as [ERROR] but will not fail the compilation.
     *
     * @since 2.4.20
     */
    STRONG_WARNING,

    /**
     * Aborts the progress of the current process (Gradle build/Import/...).
     *
     * Used *extremely* sparingly, as failing the current process can:
     * - mask further errors (forcing users to make multiple runs before fixing all issues)
     * - lead to unpleasant UX in IDE (if the failure happens during import, then depending
     *   on when it happened users might not have even basic IDE assistance, which makes fixing
     *   the root cause very annoying)
     *
     * For example, it could be used for irreconcilable misconfigurations / malformed input which prevent further
     * configuration _and_ when the graceful degradation (allowing configuration phase to finish)
     * is too expensive.
     *
     * @since 2.4.20
     */
    FATAL,
}
