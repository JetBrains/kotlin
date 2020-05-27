// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.*

class BiggestIndexedFileQueue {

  companion object {
    const val SIZE_LIMIT = 5
  }

  val biggestIndexedFiles: List<IndexedFileStat>
    get() = queue.toList()

  private val queue = PriorityQueue<IndexedFileStat>(compareBy { it.indexingTime })

  @Synchronized
  fun addFile(file: IndexedFileStat) {
    queue.add(file)
    while (queue.size > SIZE_LIMIT) {
      queue.poll()
    }
  }
}