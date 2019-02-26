/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.writer

interface ReplWriter {
    fun printlnWelcomeMessage(x: String)
    fun printlnHelpMessage(x: String)
    fun outputCommandResult(x: String)
    fun notifyReadLineStart()
    fun notifyReadLineEnd()
    fun notifyIncomplete()
    fun notifyCommandSuccess()
    fun outputCompileError(x: String)
    fun outputRuntimeError(x: String)
    fun sendInternalErrorReport(x: String)
}
