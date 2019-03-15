/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.util

import java.io.PrintWriter

class WriterBackedKaptLogger(
    override val isVerbose: Boolean,
    override val infoWriter: PrintWriter = PrintWriter(System.out),
    override val warnWriter: PrintWriter = PrintWriter(System.out),
    override val errorWriter: PrintWriter = PrintWriter(System.err)
) : KaptLogger {
    override fun info(message: String) {
        if (isVerbose) {
            report("INFO", message, infoWriter)
        }
    }

    override fun warn(message: String) {
        report("WARN", message, warnWriter)
    }

    override fun error(message: String) {
        report("ERROR", message, errorWriter)
    }

    override fun exception(e: Throwable) {
        errorWriter.println("An error occurred:")
        e.printStackTrace(errorWriter)
        errorWriter.flush()
    }

    private fun report(prefix: String, message: String, writer: PrintWriter) {
        writer.println("[$prefix] $message")
        writer.flush()
    }
}