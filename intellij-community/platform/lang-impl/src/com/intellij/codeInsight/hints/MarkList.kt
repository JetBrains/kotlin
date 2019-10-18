// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import java.util.*

class MarkList<T>(private val items: List<T>) : Iterable<T> {
  private val myMarked = BitSet(items.size)

  operator fun get(index: Int) : T = items[index]

  val size: Int
    get() = items.size

  override fun iterator(): Iterator<T> = items.iterator()

  fun mark(index: Int) {
    myMarked[index] = true
  }

  fun mark(index: Int, value: Boolean) {
    myMarked[index] = value
  }

  fun marked(index: Int): Boolean {
    return myMarked[index]
  }

  fun iterateNonMarked(consumer: (Int, T) -> Unit) {
    var i = myMarked.nextClearBit(0)
    val itemsSize = items.size
    while (i in 0 until itemsSize) {
      consumer(i, items[i])
      i = myMarked.nextClearBit(i + 1)
    }
  }
}