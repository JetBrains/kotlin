/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl

import org.jetbrains.kotlin.scripting.repl.writer.ReplWriter
import java.io.PrintWriter
import java.io.StringWriter

interface ReplExceptionReporter {
    fun report(e: Throwable)

    companion object DoNothing : ReplExceptionReporter {
        override fun report(e: Throwable) {}
    }
}

class IdeReplExceptionReporter(private val replWriter: ReplWriter) : ReplExceptionReporter {
    override fun report(e: Throwable) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)

        val writerString = stringWriter.toString()
        val internalErrorText = if (writerString.isEmpty()) "Unknown error" else writerString

        replWriter.sendInternalErrorReport(internalErrorText)
    }
}
