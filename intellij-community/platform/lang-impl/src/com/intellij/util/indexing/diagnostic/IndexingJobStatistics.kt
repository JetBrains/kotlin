// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

class IndexingJobStatistics {
  private val timeBucketSize = 128

  val timesPerIndexer: Map<String /* ID.name() */, TimeStats>
    get() = _timesPerIndexer

  val statsPerFileType: Map<String /* File type name */, StatsPerFileType>
    get() = _statsPerFileType

  data class StatsPerFileType(
    val indexingTime: TimeStats,
    val contentLoadingTime: TimeStats,
    var numberOfFiles: Int
  )

  private val _timesPerIndexer = hashMapOf<String, TimeStats>()
  private val _statsPerFileType = hashMapOf<String, StatsPerFileType>()

  @Synchronized
  fun addFileStatistics(fileStatistics: FileIndexingStatistics, contentLoadingTime: Long) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      _timesPerIndexer.getOrPut(indexId.name) { TimeStats(timeBucketSize) }.addTime(time)
    }
    val fileTypeName = fileStatistics.fileType.name
    val stats = _statsPerFileType.computeIfAbsent(fileTypeName) {
      StatsPerFileType(TimeStats(timeBucketSize), TimeStats(timeBucketSize), 0)
    }
    stats.contentLoadingTime.addTime(contentLoadingTime)
    stats.indexingTime.addTime(fileStatistics.indexingTime)
    stats.numberOfFiles++
  }
}