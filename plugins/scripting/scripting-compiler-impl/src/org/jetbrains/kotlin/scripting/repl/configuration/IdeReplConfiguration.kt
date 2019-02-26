/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.configuration

import org.jetbrains.kotlin.scripting.repl.IdeReplExceptionReporter
import org.jetbrains.kotlin.scripting.repl.ReplExceptionReporter
import org.jetbrains.kotlin.scripting.repl.messages.IdeDiagnosticMessageHolder
import org.jetbrains.kotlin.scripting.repl.reader.IdeReplCommandReader
import org.jetbrains.kotlin.scripting.repl.reader.ReplCommandReader
import org.jetbrains.kotlin.scripting.repl.reader.ReplSystemInWrapper
import org.jetbrains.kotlin.scripting.repl.writer.IdeSystemOutWrapperReplWriter
import org.jetbrains.kotlin.scripting.repl.writer.ReplWriter

class IdeReplConfiguration : ReplConfiguration {
    override val allowIncompleteLines: Boolean
        get() = false

    override val executionInterceptor: SnippetExecutionInterceptor = object :
        SnippetExecutionInterceptor {
        override fun <T> execute(block: () -> T): T {
            try {
                sinWrapper.isReplScriptExecuting = true
                return block()
            } finally {
                sinWrapper.isReplScriptExecuting = false
            }
        }
    }

    override fun createDiagnosticHolder() = IdeDiagnosticMessageHolder()

    override val writer: ReplWriter
    override val exceptionReporter: ReplExceptionReporter
    override val commandReader: ReplCommandReader

    val sinWrapper: ReplSystemInWrapper

    init {
        // wrapper for `out` is required to escape every input in [ideMode];
        // if [ideMode == false] then just redirects all input to [System.out]
        // if user calls [System.setOut(...)] then undefined behaviour
        val soutWrapper = IdeSystemOutWrapperReplWriter(System.out)
        System.setOut(soutWrapper)

        // wrapper for `in` is required to give user possibility of calling
        // [readLine] from ide-console repl
        sinWrapper = ReplSystemInWrapper(System.`in`, soutWrapper)
        System.setIn(sinWrapper)

        writer = soutWrapper
        exceptionReporter = IdeReplExceptionReporter(writer)
        commandReader = IdeReplCommandReader()
    }
}
