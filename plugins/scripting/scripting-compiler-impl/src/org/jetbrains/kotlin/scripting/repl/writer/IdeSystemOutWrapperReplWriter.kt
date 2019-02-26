/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.writer

import org.jetbrains.kotlin.cli.common.repl.replAddLineBreak
import org.jetbrains.kotlin.cli.common.repl.replOutputAsXml
import org.jetbrains.kotlin.utils.repl.ReplEscapeType
import org.jetbrains.kotlin.utils.repl.ReplEscapeType.*
import java.io.PrintStream

class IdeSystemOutWrapperReplWriter(standardOut: PrintStream) : PrintStream(standardOut, true),
    ReplWriter {
    override fun print(x: Boolean) = printWithEscaping(x.toString())
    override fun print(x: Char) = printWithEscaping(x.toString())
    override fun print(x: Int) = printWithEscaping(x.toString())
    override fun print(x: Long) = printWithEscaping(x.toString())
    override fun print(x: Float) = printWithEscaping(x.toString())
    override fun print(x: Double) = printWithEscaping(x.toString())
    override fun print(x: String) = printWithEscaping(x)
    override fun print(x: Any?) = printWithEscaping(x.toString())

    private fun printlnWithEscaping(text: String, escapeType: ReplEscapeType = USER_OUTPUT) {
        printWithEscaping("$text\n", escapeType)
    }

    private fun printWithEscaping(text: String, escapeType: ReplEscapeType = USER_OUTPUT) {
        super.print(text.replOutputAsXml(escapeType).replAddLineBreak())
    }

    override fun printlnWelcomeMessage(x: String) = printlnWithEscaping(x, INITIAL_PROMPT)
    override fun printlnHelpMessage(x: String) = printlnWithEscaping(x, HELP_PROMPT)
    override fun outputCommandResult(x: String) = printlnWithEscaping(x, REPL_RESULT)
    override fun notifyReadLineStart() = printlnWithEscaping("", READLINE_START)
    override fun notifyReadLineEnd() = printlnWithEscaping("", READLINE_END)
    override fun notifyCommandSuccess() = printlnWithEscaping("", SUCCESS)
    override fun notifyIncomplete() = printlnWithEscaping("", REPL_INCOMPLETE)
    override fun outputCompileError(x: String) = printlnWithEscaping(x, COMPILE_ERROR)
    override fun outputRuntimeError(x: String) = printlnWithEscaping(x, RUNTIME_ERROR)
    override fun sendInternalErrorReport(x: String) = printlnWithEscaping(x, INTERNAL_ERROR)
}
