// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

import com.intellij.util.indexing.diagnostic.*
import java.time.Duration

fun TimeNano.toMillis(): TimeMillis = this / 1_000_000

// Int value that is greater than zero.
// Can be used to skip int value from JSON if it is equal to 0 (to not pollute the JSON report).
typealias PositiveInt = Int?

fun Int.toPositiveInt() = takeIf { it > 0 }

fun FileProviderIndexStatistics.toJson(): JsonFileProviderIndexStatistics {
  val statsPerFileType = aggregateStatsPerFileType()
  val allStatsPerIndexer = aggregateStatsPerIndexer()
  val (statsPerIndexer, fastIndexers) = allStatsPerIndexer.partition { it.partOfTotalIndexingTime.percentages > 0.01 }

  return JsonFileProviderIndexStatistics(
    providerDebugName,
    numberOfFiles,
    JsonDuration(totalTime),
    indexingStatistics.numberOfTooLargeForIndexingFiles.get().toPositiveInt(),
    indexingStatistics.tooLargeForIndexingFiles.biggestElements.map { it.toJson() }.takeIf { it.isNotEmpty() },
    statsPerFileType.sortedByDescending { it.partOfTotalIndexingTime.percentages },
    statsPerIndexer.sortedByDescending { it.partOfTotalIndexingTime.percentages },
    fastIndexers.map { it.indexId }.sorted()
  )
}

private fun FileProviderIndexStatistics.aggregateStatsPerFileType(): List<JsonFileProviderIndexStatistics.JsonStatsPerFileType> {
  val totalIndexingTimePerFileType = indexingStatistics.statsPerFileType.values
    .filterNot { it.indexingTime.isEmpty }
    .map { it.indexingTime.sumTime }
    .sum()

  val totalContentLoadingTimePerFileType = indexingStatistics.statsPerFileType.values
    .filterNot { it.contentLoadingTime.isEmpty }
    .map { it.contentLoadingTime.sumTime }
    .sum()

  return indexingStatistics.statsPerFileType
    .mapNotNull { (fileTypeName, stats) ->
      JsonFileProviderIndexStatistics.JsonStatsPerFileType(
        fileTypeName,
        stats.numberOfFiles,
        JsonFileSize(stats.totalBytes),
        calculatePercentages(stats.indexingTime.sumTime, totalIndexingTimePerFileType),
        calculatePercentages(stats.contentLoadingTime.sumTime, totalContentLoadingTimePerFileType)
      )
    }
}

private fun FileProviderIndexStatistics.aggregateStatsPerIndexer(): List<JsonFileProviderIndexStatistics.JsonStatsPerIndexer> {
  val totalIndexingTimePerIndexer = indexingStatistics.statsPerIndexer.values
    .filterNot { it.indexingTime.isEmpty }
    .map { it.indexingTime.sumTime }
    .sum()

  return indexingStatistics.statsPerIndexer
    .mapNotNull { (indexId, stats) ->
      JsonFileProviderIndexStatistics.JsonStatsPerIndexer(
        indexId,
        calculatePercentages(stats.indexingTime.sumTime, totalIndexingTimePerIndexer)
      )
    }
}

fun ProjectIndexingHistory.IndexingTimes.toJson() =
  JsonProjectIndexingHistoryTimes(
    JsonDuration(Duration.between(indexingStart, indexingEnd).toNanos()),
    JsonDuration(Duration.between(scanFilesStart, scanFilesEnd).toNanos()),
    JsonDuration(Duration.between(pushPropertiesStart, pushPropertiesEnd).toNanos()),
    JsonDuration(Duration.between(indexExtensionsStart, indexExtensionsEnd).toNanos()),
    JsonDateTime(pushPropertiesStart!!),
    JsonDateTime(pushPropertiesEnd!!),
    JsonDateTime(scanFilesStart!!),
    JsonDateTime(scanFilesEnd!!),
    JsonDateTime(indexExtensionsStart!!),
    JsonDateTime(indexExtensionsEnd!!),
    JsonDateTime(indexingStart!!),
    JsonDateTime(indexingEnd!!)
  )

private fun calculatePercentages(part: Long, total: Long): JsonPercentages =
  if (total == 0L) {
    JsonPercentages(1.0)
  }
  else {
    JsonPercentages(part.toDouble() / total)
  }

fun ProjectIndexingHistory.toJson() =
  JsonProjectIndexingHistory(
    projectName,
    providerStatistics.size,
    aggregateTotalNumberOfFiles(),
    times.toJson(),
    numberOfIndexingThreads,
    totalNumberOfTooLargeFiles.toPositiveInt(),
    totalTooLargeFiles.biggestElements.map { it.toJson() }.takeIf { it.isNotEmpty() },
    aggregateStatsPerFileType().sortedByDescending { it.partOfTotalIndexingTime.percentages },
    aggregateStatsPerIndexer().sortedByDescending { it.partOfTotalIndexingTime.percentages },
    providerStatistics.sortedByDescending { it.totalIndexingTime.nano }
  )

private fun ProjectIndexingHistory.aggregateTotalNumberOfFiles() =
  providerStatistics.map { it.totalNumberOfFiles }.sum()

private fun ProjectIndexingHistory.aggregateStatsPerFileType(): List<JsonProjectIndexingHistory.JsonStatsPerFileType> {
  val totalIndexingTime = totalStatsPerFileType.values.map { it.totalIndexingTimeInAllThreads }.sum()
  val fileTypeToIndexingTimePart = totalStatsPerFileType.mapValues {
    calculatePercentages(it.value.totalIndexingTimeInAllThreads, totalIndexingTime)
  }

  @Suppress("DuplicatedCode")
  val totalContentLoadingTime = totalStatsPerFileType.values.map { it.totalContentLoadingTimeInAllThreads }.sum()
  val fileTypeToContentLoadingTimePart = totalStatsPerFileType.mapValues {
    calculatePercentages(it.value.totalContentLoadingTimeInAllThreads, totalContentLoadingTime)
  }

  val fileTypeToProcessingSpeed = totalStatsPerFileType.mapValues {
    JsonProcessingSpeed(it.value.totalBytes, it.value.totalIndexingTimeInAllThreads)
  }

  return totalStatsPerFileType.map { (fileType, stats) ->
    val jsonBiggestFileTypeContributors = stats.biggestFileTypeContributors.biggestElements.map {
      JsonProjectIndexingHistory.JsonStatsPerFileType.JsonBiggestFileTypeContributor(
        it.providerName,
        it.numberOfFiles,
        JsonFileSize(it.totalBytes),
        calculatePercentages(it.indexingTimeInAllThreads, stats.totalIndexingTimeInAllThreads)
      )
    }
    JsonProjectIndexingHistory.JsonStatsPerFileType(
      fileType,
      fileTypeToIndexingTimePart.getValue(fileType),
      fileTypeToContentLoadingTimePart.getValue(fileType),
      stats.totalNumberOfFiles,
      JsonFileSize(stats.totalBytes),
      fileTypeToProcessingSpeed.getValue(fileType),
      jsonBiggestFileTypeContributors.sortedByDescending { it.partOfTotalIndexingTimeOfThisFileType.percentages }
    )
  }
}

private fun TooLargeForIndexingFile.toJson() = JsonTooLargeForIndexingFile(fileName, JsonFileSize(fileSize))

private fun ProjectIndexingHistory.aggregateStatsPerIndexer(): List<JsonProjectIndexingHistory.JsonStatsPerIndexer> {
  val totalIndexingTime = totalStatsPerIndexer.values.map { it.totalIndexingTimeInAllThreads }.sum()
  val indexIdToIndexingTimePart = totalStatsPerIndexer.mapValues {
    calculatePercentages(it.value.totalIndexingTimeInAllThreads, totalIndexingTime)
  }

  val indexIdToProcessingSpeed = totalStatsPerIndexer.mapValues {
    JsonProcessingSpeed(it.value.totalBytes, it.value.totalIndexingTimeInAllThreads)
  }

  return totalStatsPerIndexer.map { (indexId, stats) ->
    JsonProjectIndexingHistory.JsonStatsPerIndexer(
      indexId,
      indexIdToIndexingTimePart.getValue(indexId),
      stats.totalNumberOfFiles,
      JsonFileSize(stats.totalBytes),
      indexIdToProcessingSpeed.getValue(indexId)
    )
  }
}