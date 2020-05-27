// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

typealias TimeMillis = Long
typealias TimeNano = Long
typealias BytesNumber = Long

data class ProjectIndexingHistory(val projectName: String) {
  val times: IndexingTimes = IndexingTimes()

  val providerStatistics: MutableList<JsonFileProviderIndexStatistics> = arrayListOf()

  val totalStatsPerFileType: Map<String /* File type name */, StatsPerFileType>
    get() = _totalStatsPerFileType

  val totalStatsPerIndexer: Map<String /* Index ID */, StatsPerIndexer>
    get() = _totalStatsPerIndexer

  private val _totalStatsPerFileType = hashMapOf<String, StatsPerFileType>()

  private val _totalStatsPerIndexer = hashMapOf<String, StatsPerIndexer>()

  @Synchronized
  fun addProviderStatistics(statistics: FileProviderIndexStatistics) {
    // Convert to Json to release memory occupied by statistic values.
    providerStatistics += statistics.convertToJson()

    for ((fileType, fileTypeStats) in statistics.indexingStatistics.statsPerFileType) {
      val totalStats = _totalStatsPerFileType.getOrPut(fileType) { StatsPerFileType(0, 0, 0, 0, BiggestIndexedFileQueue()) }
      totalStats.totalNumberOfFiles += fileTypeStats.numberOfFiles
      totalStats.totalBytes += fileTypeStats.totalBytes
      totalStats.totalIndexingTimeInAllThreads += fileTypeStats.indexingTime.sumTime
      totalStats.totalContentLoadingTimeInAllThreads += fileTypeStats.contentLoadingTime.sumTime
      fileTypeStats.biggestContributors.biggestIndexedFiles.forEach {
        totalStats.biggestContributors.addFile(IndexedFileStat(it.fileName, it.fileType, it.fileSize, it.indexingTime))
      }
    }

    for ((indexId, stats) in statistics.indexingStatistics.statsPerIndexer) {
      val totalStats = _totalStatsPerIndexer.getOrPut(indexId) { StatsPerIndexer(0, 0, 0) }
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
    val biggestContributors: BiggestIndexedFileQueue
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