// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.intellij.openapi.fileTypes.FileType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

class IndexingJobStatistics {
  private val timeBucketSize = 1024

  val timesPerIndexer: ConcurrentMap<String /* ID.name() */, MaxNTimeBucket> = ConcurrentHashMap()
  val timesPerFileType: ConcurrentMap<String /* File type name */, MaxNTimeBucket> = ConcurrentHashMap()
  val numberOfFilesPerFileType: ConcurrentMap<String /* File type name */, Int> = ConcurrentHashMap()
  val contentLoadingTime = AtomicReference<MaxNTimeBucket>()
  val indexingTime = AtomicReference<MaxNTimeBucket>()

  fun addFileStatistics(fileStatistics: FileIndexingStatistics, fileType: FileType) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      timesPerIndexer.computeIfAbsent(indexId.name) { MaxNTimeBucket(timeBucketSize, time) }.addTime(time)
    }
    val fileTypeName = fileType.name
    numberOfFilesPerFileType.compute(fileTypeName) { _, currentNumber -> (currentNumber ?: 0) + 1 }
    timesPerFileType.computeIfAbsent(fileTypeName) { MaxNTimeBucket(timeBucketSize, fileStatistics.totalTime) }.addTime(fileStatistics.totalTime)
  }

  fun addIndexingTime(nanoTime: TimeNano) {
    indexingTime.updateAndGet { bucket -> bucket ?: MaxNTimeBucket(timeBucketSize, nanoTime) }.addTime(nanoTime)
  }

  fun addContentLoadingTime(nanoTime: TimeNano) {
    contentLoadingTime.updateAndGet { bucket -> bucket ?: MaxNTimeBucket(timeBucketSize, nanoTime) }.addTime(nanoTime)
  }
}