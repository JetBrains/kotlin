// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

class IndexingJobStatistics {

  val statsPerIndexer = hashMapOf<String /* ID.name() */, StatsPerIndexer>()

  val statsPerFileType = hashMapOf<String /* File type name */, StatsPerFileType>()

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
      val stats = statsPerIndexer.getOrPut(indexId.name) {
        StatsPerIndexer(TimeStats(), 0, 0)
      }
      stats.indexingTime.addTime(time)
      stats.numberOfFiles++
      stats.totalBytes += fileSize
    }
    val fileTypeName = fileStatistics.fileType.name
    val stats = statsPerFileType.computeIfAbsent(fileTypeName) {
      StatsPerFileType(TimeStats(), TimeStats(), 0, 0)
    }
    stats.contentLoadingTime.addTime(contentLoadingTime)
    stats.indexingTime.addTime(fileStatistics.indexingTime)
    stats.totalBytes += fileSize
    stats.numberOfFiles++
  }
}