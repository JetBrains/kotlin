// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.intellij.openapi.fileTypes.FileType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class IndexingJobStatistics {
  val timesPerIndexer: ConcurrentMap<String, PerThreadTime> = ConcurrentHashMap()
  val timesPerFileType: ConcurrentMap<String, PerThreadTime> = ConcurrentHashMap()
  val numberOfFilesPerFileType: ConcurrentMap<String, Int> = ConcurrentHashMap()
  val contentLoadingTime = PerThreadTime()
  val indexingTime = PerThreadTime()

  fun addFileStatistics(fileStatistics: FileIndexingStatistics, fileType: FileType) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      timesPerFileType.computeIfAbsent(indexId.name) { PerThreadTime() }
        .addTimeSpentInCurrentThread(time)
    }
    val fileTypeName = fileType.name
    numberOfFilesPerFileType.compute(fileTypeName) { _, currentNumber -> (currentNumber ?: 0) + 1 }
    timesPerFileType
      .computeIfAbsent(fileTypeName) { PerThreadTime() }
      .addTimeSpentInCurrentThread(fileStatistics.totalTime)
  }

  fun addIndexingTime(nanoTime: TimeNano) {
    indexingTime.addTimeSpentInCurrentThread(nanoTime)
  }

  fun addContentLoadingTime(nanoTime: TimeNano) {
    contentLoadingTime.addTimeSpentInCurrentThread(nanoTime)
  }
}