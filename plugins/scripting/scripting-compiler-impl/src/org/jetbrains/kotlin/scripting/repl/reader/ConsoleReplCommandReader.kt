/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.reader

import org.jetbrains.kotlin.scripting.repl.ReplFromTerminal
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class ConsoleReplCommandReader : ReplCommandReader {
    private val lineReader = LineReaderBuilder.builder()
        .appName("kotlin")
        .terminal(TerminalBuilder.terminal())
        .variable(LineReader.HISTORY_FILE, File(File(System.getProperty("user.home")), ".kotlinc_history").absolutePath)
        .build()
        .apply {
            setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION)
        }

    override fun readLine(next: ReplFromTerminal.WhatNextAfterOneLine): String? {
        val prompt = if (next == ReplFromTerminal.WhatNextAfterOneLine.INCOMPLETE) "... " else ">>> "
        return try {
            lineReader.readLine(prompt)
        } catch (e: UserInterruptException) {
            println("<interrupted>")
            System.out.flush()
            ""
        } catch (e: EndOfFileException) {
            null
        }
    }

    override fun flushHistory() = lineReader.history.save()

    private companion object {
        init {
            Logger.getLogger("org.jline").level = Level.OFF
        }
    }
}
