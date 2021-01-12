// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import java.io.Closeable

abstract class LineProcessor : Appendable, Closeable {
  private var lineBuilder: StringBuilder? = StringBuilder()

  abstract fun process(line: String)

  override fun append(csq: CharSequence): LineProcessor {
    for (element in csq) {
      append(element)
    }
    return this
  }

  override fun append(csq: CharSequence, start: Int, end: Int): LineProcessor {
    append(csq.subSequence(start, end))
    return this
  }

  override fun append(c: Char): LineProcessor {
    if (lineBuilder == null) throw IllegalStateException("The line processor was closed")
    if (c == '\n') {
      if (lineBuilder!!.lastOrNull() == '\r') {
        lineBuilder!!.deleteCharAt(lineBuilder!!.length - 1)
      }
      flushBuffer()
    }
    else {
      lineBuilder!!.append(c)
    }
    return this
  }

  override fun close() {
    if (lineBuilder != null) {
      flushBuffer()
      lineBuilder = null
    }
  }

  private fun flushBuffer() {
    val line = lineBuilder!!.toString()
    lineBuilder!!.setLength(0)
    process(line)
  }
}
