/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration

import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplExceptionReporter
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.messages.ConsoleDiagnosticMessageHolder
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.reader.ConsoleReplCommandReader
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.writer.ConsoleReplWriter

class ConsoleReplConfiguration : ReplConfiguration {
    override val writer = ConsoleReplWriter()

    override val exceptionReporter
        get() = ReplExceptionReporter

    override val commandReader = ConsoleReplCommandReader()

    override val allowIncompleteLines: Boolean
        get() = true

    override val executionInterceptor
        get() = SnippetExecutionInterceptor

    override fun createDiagnosticHolder() = ConsoleDiagnosticMessageHolder()
}
