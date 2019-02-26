/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.writer

class ConsoleReplWriter : ReplWriter {
    override fun printlnWelcomeMessage(x: String) = println(x)
    override fun printlnHelpMessage(x: String) = println(x)
    override fun outputCompileError(x: String) = println(x)
    override fun outputCommandResult(x: String) = println(x)
    override fun outputRuntimeError(x: String) = println(x)

    override fun notifyReadLineStart() {}
    override fun notifyReadLineEnd() {}
    override fun notifyIncomplete() {}
    override fun notifyCommandSuccess() {}
    override fun sendInternalErrorReport(x: String) {}
}
