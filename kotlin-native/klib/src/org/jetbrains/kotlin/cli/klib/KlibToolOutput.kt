/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

/**
 * TODO: Consider using [org.jetbrains.kotlin.cli.common.messages.MessageCollector] instead of this class.
 *  Note: The [org.jetbrains.kotlin.cli.common.messages.MessageCollector] is designed to track an individual short messages
 *  logged at different levels, but is not supposed to collect plain output (such as, for example, IR dump).
 */
internal class KlibToolOutput(
        stdout: Appendable,
        val stderr: Appendable
) : Appendable by stdout {
    var hasErrors: Boolean = false
        private set

    internal fun logInfo(text: String) {
        stderr.append("info: ").appendLine(text)
    }

    internal fun logWarning(text: String) {
        stderr.append("warning: ").appendLine(text)
    }

    internal fun logError(text: String) {
        hasErrors = true
        stderr.append("error: ").appendLine(text)
    }

    internal fun logErrorWithStackTrace(t: Throwable) {
        hasErrors = true
        stderr.appendLine(t.stackTraceToString())
    }
}
