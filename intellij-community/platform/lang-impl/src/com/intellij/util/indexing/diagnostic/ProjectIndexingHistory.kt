// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.intellij.util.indexing.diagnostic.dto.JsonFileProviderIndexStatistics
import com.intellij.util.indexing.diagnostic.dto.toJson
import java.time.Instant

typealias TimeMillis = Long
typealias TimeNano = Long
typealias BytesNumber = Long

data class ProjectIndexingHistory(val projectName: String) {
  private val biggestContributorsPerFileTypeLimit = 10

  val times = IndexingTimes()

  var numberOfIndexingThreads: Int = 0

  val providerStatistics = arrayListOf<JsonFileProviderIndexStatistics>()

  val totalStatsPerFileType = hashMapOf<String /* File type name */, StatsPerFileType>()

  val totalStatsPerIndexer = hashMapOf<String /* Index ID */, StatsPerIndexer>()

  var totalNumberOfTooLargeFiles: Int = 0
  val totalTooLargeFiles = LimitedPriorityQueue<TooLargeForIndexingFile>(5, compareBy { it.fileSize })

  fun addProviderStatistics(statistics: FileProviderIndexStatistics) {
    // Convert to Json to release memory occupied by statistic values.
    providerStatistics += statistics.toJson()

    for ((fileType, fileTypeStats) in statistics.indexingStatistics.statsPerFileType) {
      val totalStats = totalStatsPerFileType.getOrPut(fileType) {
        StatsPerFileType(0, 0, 0, 0, LimitedPriorityQueue(biggestContributorsPerFileTypeLimit, compareBy { it.indexingTimeInAllThreads }))
      }
      totalStats.totalNumberOfFiles += fileTypeStats.numberOfFiles
      totalStats.totalBytes += fileTypeStats.totalBytes
      totalStats.totalIndexingTimeInAllThreads += fileTypeStats.indexingTime.sumTime
      totalStats.totalContentLoadingTimeInAllThreads += fileTypeStats.contentLoadingTime.sumTime
      totalStats.biggestFileTypeContributors.addElement(
        BiggestFileTypeContributor(
          statistics.providerDebugName,
          fileTypeStats.numberOfFiles,
          fileTypeStats.totalBytes,
          fileTypeStats.indexingTime.sumTime
        )
      )
    }

    for ((indexId, stats) in statistics.indexingStatistics.statsPerIndexer) {
      val totalStats = totalStatsPerIndexer.getOrPut(indexId) { StatsPerIndexer(0, 0, 0) }
      totalStats.totalNumberOfFiles += stats.numberOfFiles
      totalStats.totalBytes += stats.totalBytes
      totalStats.totalIndexingTimeInAllThreads += stats.indexingTime.sumTime
    }

    totalNumberOfTooLargeFiles += statistics.indexingStatistics.numberOfTooLargeForIndexingFiles.get()
    statistics.indexingStatistics.tooLargeForIndexingFiles.biggestElements.forEach { totalTooLargeFiles.addElement(it) }
  }

  data class StatsPerFileType(
    var totalNumberOfFiles: Int,
    var totalBytes: BytesNumber,
    var totalIndexingTimeInAllThreads: TimeNano,
    var totalContentLoadingTimeInAllThreads: TimeNano,
    val biggestFileTypeContributors: LimitedPriorityQueue<BiggestFileTypeContributor>
  )

  data class BiggestFileTypeContributor(
    val providerName: String,
    val numberOfFiles: Int,
    val totalBytes: BytesNumber,
    val indexingTimeInAllThreads: TimeNano
  )

  data class StatsPerIndexer(
    var totalNumberOfFiles: Int,
    var totalBytes: BytesNumber,
    var totalIndexingTimeInAllThreads: TimeNano
  )

  data class IndexingTimes(
    var indexingStart: Instant? = null,
    var indexingEnd: Instant? = null,
    var pushPropertiesStart: Instant? = null,
    var pushPropertiesEnd: Instant? = null,
    var indexExtensionsStart: Instant? = null,
    var indexExtensionsEnd: Instant? = null,
    var scanFilesStart: Instant? = null,
    var scanFilesEnd: Instant? = null
  )
}