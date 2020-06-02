// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import java.util.concurrent.atomic.AtomicInteger

class IndexingJobStatistics {

  private val _statsPerIndexer = hashMapOf<String, StatsPerIndexer>()
  val statsPerIndexer: Map<String /* ID.name() */, StatsPerIndexer>
    @Synchronized get() = _statsPerIndexer.toMap()

  private val _statsPerFileType = hashMapOf<String, StatsPerFileType>()
  val statsPerFileType: Map<String /* File type name */, StatsPerFileType>
    @Synchronized get() = _statsPerFileType.toMap()

  val numberOfTooLargeForIndexingFiles = AtomicInteger()
  val tooLargeForIndexingFiles = LimitedPriorityQueue<TooLargeForIndexingFile>(5, compareBy { it.fileSize })

  data class StatsPerIndexer(
    val indexingTime: TimeStats,
    var numberOfFiles: Int,
    var totalBytes: BytesNumber
  )

  data class StatsPerFileType(
    val indexingTime: TimeStats,
    val contentLoadingTime: TimeStats,
    var numberOfFiles: Int,
    var totalBytes: BytesNumber
  )

  @Synchronized
  fun addFileStatistics(
    fileStatistics: FileIndexingStatistics,
    contentLoadingTime: Long,
    fileSize: Long
  ) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      val stats = _statsPerIndexer.getOrPut(indexId.name) {
        StatsPerIndexer(TimeStats(), 0, 0)
      }
      stats.indexingTime.addTime(time)
      stats.numberOfFiles++
      stats.totalBytes += fileSize
    }
    val fileTypeName = fileStatistics.fileType.name
    val stats = _statsPerFileType.getOrPut(fileTypeName) {
      StatsPerFileType(TimeStats(), TimeStats(), 0, 0)
    }
    stats.contentLoadingTime.addTime(contentLoadingTime)
    stats.indexingTime.addTime(fileStatistics.indexingTime)
    stats.totalBytes += fileSize
    stats.numberOfFiles++
  }
}