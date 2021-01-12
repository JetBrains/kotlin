// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.*

class LimitedPriorityQueue<T>(private val sizeLimit: Int, comparator: Comparator<T>) {

  val biggestElements: List<T>
    @Synchronized get() = queue.toList()

  private val queue = PriorityQueue(comparator)

  @Synchronized
  fun addElement(element: T) {
    queue.add(element)
    while (queue.size > sizeLimit) {
      queue.poll()
    }
  }
}