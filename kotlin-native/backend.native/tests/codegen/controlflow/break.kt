/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.`break`

import kotlin.test.*

fun foo() {
  var i = 0
  l1@while (true) {
    println("foo@l1")
    try {
      l2@while (true) {
        if ((i++ % 2) == 0) continue@l2
        println("foo@l2")
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
      println("bar@l1")
      throw Exception()
    } catch (e: Exception) {
      l2@do {
        if ((i++ % 2) == 0) continue@l2
        println("bar@l2")
        if (i > 4) break@l1
      } while (true)
    }
  } while (true)
}

fun qux() {
  l1@for (i in 1..6) {
    t1@try {
      println("qux@t1")
      throw Exception()
    }
    finally {
      l2@ for (j in 1..6) {
        if ((j % 2) == 0) continue@l2
        println("qux@l2")
        if (j > 4) break@l1
      }
    }
  }
}

@Test fun runTest() {
  foo()
  bar()
  qux()
}
