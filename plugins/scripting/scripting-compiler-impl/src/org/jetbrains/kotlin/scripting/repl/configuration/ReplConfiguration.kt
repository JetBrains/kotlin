/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.configuration

import org.jetbrains.kotlin.scripting.repl.ReplExceptionReporter
import org.jetbrains.kotlin.scripting.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.scripting.repl.reader.ReplCommandReader
import org.jetbrains.kotlin.scripting.repl.writer.ReplWriter

interface ReplConfiguration {
    val writer: ReplWriter
    val exceptionReporter: ReplExceptionReporter
    val commandReader: ReplCommandReader
    val allowIncompleteLines: Boolean

    val executionInterceptor: SnippetExecutionInterceptor
    fun createDiagnosticHolder(): DiagnosticMessageHolder
}
