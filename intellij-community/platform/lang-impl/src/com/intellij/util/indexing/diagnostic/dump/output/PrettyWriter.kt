package com.intellij.util.indexing.diagnostic.dump.output

import java.io.PrintStream

class PrettyWriter(private val out: PrintStream) : AutoCloseable {

  private var indent = ""

  inner class Object {
    fun writeString(s: String) {
      s.lineSequence().forEach {
        out.println("$indent$it")
      }
    }
  }

  fun writeObject(name: String, block: Object.() -> Unit) {
    out.println("$indent$name")
    indent += "  "
    val obj = Object()
    try {
      obj.block()
    }
    finally {
      indent = indent.removeSuffix("  ")
    }
  }

  override fun close() {
    out.close()
  }
}