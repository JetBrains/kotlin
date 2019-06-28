/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testing.internal

import java.io.PrintStream
import java.io.PrintWriter

class MultiCauseException(message: String, val causes: List<Error>) : Error(message, causes.firstOrNull()) {
    override fun printStackTrace(printStream: PrintStream) {
        val writer = PrintWriter(printStream)
        this.printStackTrace(writer)
        writer.flush()
    }

    override fun printStackTrace(printWriter: PrintWriter) {
        if (this.causes.size <= 1) {
            super.printStackTrace(printWriter)
        } else {
            super.printStackTrace(printWriter)

            causes.forEachIndexed { i, it ->
                printWriter.format("Cause %s: ", i + 1)
                it.printStackTrace(printWriter)
            }
        }
    }
}
