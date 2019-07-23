/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration

import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplExceptionReporter
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.reader.ReplCommandReader
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.writer.ReplWriter

interface ReplConfiguration {
    val writer: ReplWriter
    val exceptionReporter: ReplExceptionReporter
    val commandReader: ReplCommandReader
    val allowIncompleteLines: Boolean

    val executionInterceptor: SnippetExecutionInterceptor
    fun createDiagnosticHolder(): DiagnosticMessageHolder
}
