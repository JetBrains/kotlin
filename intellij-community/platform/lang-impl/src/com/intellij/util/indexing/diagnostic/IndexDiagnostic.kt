// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.intellij.util.indexing.diagnostic.dto.JsonFileProviderIndexStatistics
import com.intellij.util.indexing.diagnostic.dto.toJson

typealias TimeMillis = Long
typealias TimeNano = Long
typealias BytesNumber = Long

data class ProjectIndexingHistory(val projectName: String) {
  private val biggestContributorsLimit = 5

  val times = IndexingTimes()

  var numberOfIndexingThreads: Int = 0

  val providerStatistics = arrayListOf<JsonFileProviderIndexStatistics>()

  val totalStatsPerFileType = hashMapOf<String /* File type name */, StatsPerFileType>()

  val totalStatsPerIndexer = hashMapOf<String /* Index ID */, StatsPerIndexer>()

  @Synchronized
  fun addProviderStatistics(statistics: FileProviderIndexStatistics) {
    // Convert to Json to release memory occupied by statistic values.
    providerStatistics += statistics.toJson()

    for ((fileType, fileTypeStats) in statistics.indexingStatistics.statsPerFileType) {
      val totalStats = totalStatsPerFileType.getOrPut(fileType) {
        StatsPerFileType(0, 0, 0, 0, LimitedPriorityQueue(biggestContributorsLimit, compareBy { it.indexingTime }))
      }
      totalStats.totalNumberOfFiles += fileTypeStats.numberOfFiles
      totalStats.totalBytes += fileTypeStats.totalBytes
      totalStats.totalIndexingTimeInAllThreads += fileTypeStats.indexingTime.sumTime
      totalStats.totalContentLoadingTimeInAllThreads += fileTypeStats.contentLoadingTime.sumTime
      fileTypeStats.biggestContributors.biggestElements.forEach {
        totalStats.biggestContributors.addFile(it)
      }
    }

    for ((indexId, stats) in statistics.indexingStatistics.statsPerIndexer) {
      val totalStats = totalStatsPerIndexer.getOrPut(indexId) { StatsPerIndexer(0, 0, 0) }
      totalStats.totalNumberOfFiles += stats.numberOfFiles
      totalStats.totalBytes += stats.totalBytes
      totalStats.totalIndexingTimeInAllThreads += stats.indexingTime.sumTime
    }
  }

  data class StatsPerFileType(
    var totalNumberOfFiles: Int,
    var totalBytes: BytesNumber,
    var totalIndexingTimeInAllThreads: TimeNano,
    var totalContentLoadingTimeInAllThreads: TimeNano,
    val biggestContributors: LimitedPriorityQueue<IndexedFileStat>
  )

  data class StatsPerIndexer(
    var totalNumberOfFiles: Int,
    var totalBytes: BytesNumber,
    var totalIndexingTimeInAllThreads: TimeNano
  )

  data class IndexingTimes(
    var startIndexing: TimeMillis = 0,
    var endIndexing: TimeMillis = 0,

    var startPushProperties: TimeMillis = 0,
    var endPushProperties: TimeMillis = 0,

    var startIndexExtensions: TimeMillis = 0,
    var endIndexExtensions: TimeMillis = 0,

    var startScanFiles: TimeMillis = 0,
    var endScanFiles: TimeMillis = 0
  )
}