// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import java.io.Closeable

abstract class LineProcessor : Appendable, Closeable {
  private var lineBuilder: StringBuilder? = null

  abstract fun process(line: String)

  override fun append(csq: CharSequence): LineProcessor {
    for (i in 0 until csq.length) {
      append(csq[i])
    }
    return this
  }

  override fun append(csq: CharSequence, start: Int, end: Int): LineProcessor {
    append(csq.subSequence(start, end))
    return this
  }

  override fun append(c: Char): LineProcessor {
    if (lineBuilder == null) {
      lineBuilder = StringBuilder()
    }
    if (c == '\n') {
      flushBuffer()
    }
    else {
      lineBuilder!!.append(c)
    }
    return this
  }

  override fun close() {
    flushBuffer()
  }

  private fun flushBuffer() {
    val line = lineBuilder?.toString() ?: return
    lineBuilder!!.setLength(0)
    process(line)
  }
}
