// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

class IndexingJobStatistics {
  private val timeBucketSize = 128

  val statsPerIndexer: Map<String /* ID.name() */, StatsPerIndexer>
    get() = _statsPerIndexer

  val statsPerFileType: Map<String /* File type name */, StatsPerFileType>
    get() = _statsPerFileType

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

  private val _statsPerIndexer = hashMapOf<String, StatsPerIndexer>()
  private val _statsPerFileType = hashMapOf<String, StatsPerFileType>()

  @Synchronized
  fun addFileStatistics(
    fileStatistics: FileIndexingStatistics,
    contentLoadingTime: Long,
    fileSize: Long
  ) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      val stats = _statsPerIndexer.getOrPut(indexId.name) {
        StatsPerIndexer(TimeStats(timeBucketSize), 0, 0)
      }
      stats.indexingTime.addTime(time)
      stats.numberOfFiles++
      stats.totalBytes += fileSize
    }
    val fileTypeName = fileStatistics.fileType.name
    val stats = _statsPerFileType.computeIfAbsent(fileTypeName) {
      StatsPerFileType(TimeStats(timeBucketSize), TimeStats(timeBucketSize), 0, 0)
    }
    stats.contentLoadingTime.addTime(contentLoadingTime)
    stats.indexingTime.addTime(fileStatistics.indexingTime)
    stats.totalBytes += fileSize
    stats.numberOfFiles++
  }
}