/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun foo() {
  var i = 0
  l1@while (true) {
    sb.appendLine("foo@l1")
    try {
      l2@while (true) {
        if ((i++ % 2) == 0) continue@l2
        sb.appendLine("foo@l2")
        if (i > 4) break@l1
      }
    } finally {
    }
  }
}

fun bar() {
  var i = 0
  l1@do {
    try {
      sb.appendLine("bar@l1")
      throw Exception()
    } catch (e: Exception) {
      l2@do {
        if ((i++ % 2) == 0) continue@l2
        sb.appendLine("bar@l2")
        if (i > 4) break@l1
      } while (true)
    }
  } while (true)
}

fun qux() {
  l1@for (i in 1..6) {
    t1@try {
      sb.appendLine("qux@t1")
      throw Exception()
    }
    finally {
      l2@ for (j in 1..6) {
        if ((j % 2) == 0) continue@l2
        sb.appendLine("qux@l2")
        if (j > 4) break@l1
      }
    }
  }
}

fun box(): String {
  foo()
  bar()
  qux()

  assertEquals("""
    foo@l1
    foo@l2
    foo@l2
    foo@l2
    bar@l1
    bar@l2
    bar@l2
    bar@l2
    qux@t1
    qux@l2
    qux@l2
    qux@l2

    """.trimIndent(), sb.toString())
  return "OK"
}
