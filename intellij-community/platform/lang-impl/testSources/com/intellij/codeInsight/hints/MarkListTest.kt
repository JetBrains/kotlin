// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import junit.framework.TestCase


internal class MarkListTest : TestCase() {
  fun testMark() {
    val markList = MarkList(listOf(0, 1, 2, 3))
    markList.mark(0)
    markList.mark(3)

    assertEquals(listOf(1 to 1, 2 to 2), markList.nonUsed())
  }

  fun testEmpty() {
    val markList = MarkList<Int>(listOf())
    assertEquals(listOf<Int>(), markList.nonUsed())
  }

  fun testMarked() {
    val markList = MarkList(listOf(1, 2, 3))
    assertFalse(markList.marked(0))
    markList.mark(0)
    assertTrue(markList.marked(0))
  }
}

private fun <T> MarkList<T>.nonUsed() : List<Pair<Int, T>> {
  val list = mutableListOf<Pair<Int, T>>()
  iterateNonMarked { i, item ->
    list.add(i to item)
  }
  return list
}