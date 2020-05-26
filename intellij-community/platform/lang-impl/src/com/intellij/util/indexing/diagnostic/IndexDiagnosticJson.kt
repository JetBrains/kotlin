// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.indexing.diagnostic.JsonFileProviderIndexStatistics.Companion.FAST_INDEXER_THRESHOLD_NANO
import com.intellij.util.text.DateFormatUtil
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private fun TimeNano.toMillis(): TimeMillis = this / 1_000_000

private fun TimeMillis.toNano(): TimeNano = this * 1_000_000

@JsonSerialize(using = JsonTime.Companion::class)
data class JsonTime(val nano: Long) {
  companion object : JsonSerializer<JsonTime>() {
    override fun serialize(value: JsonTime, gen: JsonGenerator, serializers: SerializerProvider?) {
      val duration = if (value.nano < TimeUnit.MILLISECONDS.toNanos(1)) {
        "< 1 ms"
      } else {
        StringUtil.formatDuration(value.nano.toMillis())
      }
      gen.writeString(duration)
    }
  }
}

data class JsonTimeStats(
  val minTime: JsonTime,
  val maxTime: JsonTime,
  val meanTime: JsonTime,
  val medianTime: JsonTime
)

fun MaxNTimeBucket.toTimeStats(): JsonTimeStats? {
  if (isEmpty) {
    return null
  }
  return JsonTimeStats(
    JsonTime(minTime),
    JsonTime(maxTime),
    JsonTime(meanTime.toLong()),
    JsonTime(getMedianOfArray(maxNTimes).toLong())
  )
}

private fun <N : Number> getMedianOfArray(elements: Collection<N>): Double {
  require(elements.isNotEmpty())
  val sorted = elements.map { it.toDouble() }.sorted()
  return if (sorted.size % 2 == 0) {
    (sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2.0
  }
  else {
    sorted[sorted.size / 2]
  }
}

data class JsonFileProviderIndexStatistics(
  val providerName: String,
  val totalNumberOfFiles: Int,
  // <total time> = <content loading time> + <indexing time> + <time spent on waiting for other indexing tasks to complete>
  val totalIndexingTime: JsonTime,
  val statsPerFileType: List<StatsPerFileType>,
  val statsPerIndexer: List<StatsPerIndexer>,
  val fastIndexers: List<String>
) {

  companion object {
    val FAST_INDEXER_THRESHOLD_NANO = TimeUnit.MILLISECONDS.toNanos(1)
  }

  data class StatsPerFileType(
    val fileType: String,
    val numberOfFiles: Int,
    val indexingTimeStats: JsonTimeStats,
    val contentLoadingTimeStats: JsonTimeStats
  )

  data class StatsPerIndexer(
    val indexId: String,
    val indexingTimeStats: JsonTimeStats
  )
}

fun FileProviderIndexStatistics.convertToJson(): JsonFileProviderIndexStatistics {
  val statsPerFileType = indexingStatistics.numberOfFilesPerFileType
    .mapNotNull { (fileTypeName, numberOfFiles) ->
      val indexingTimeStats = indexingStatistics.indexingTimesPerFileType[fileTypeName]?.toTimeStats() ?: return@mapNotNull null
      val contentLoadingTimeStats = indexingStatistics.contentLoadingTimesPerFileType[fileTypeName]?.toTimeStats() ?: return@mapNotNull null
      JsonFileProviderIndexStatistics.StatsPerFileType(fileTypeName, numberOfFiles, indexingTimeStats, contentLoadingTimeStats)
    }

  val totalNumberOfFiles = statsPerFileType.asSequence().map { it.numberOfFiles }.sum()
  val allStatsPerIndexer = indexingStatistics.timesPerIndexer
    .mapNotNull {
      val timeStats = it.value.toTimeStats() ?: return@mapNotNull null
      JsonFileProviderIndexStatistics.StatsPerIndexer(it.key, timeStats)
    }
    .sortedByDescending { it.indexingTimeStats.maxTime.nano }
  val (statsPerIndexer, fastIndexers) = allStatsPerIndexer.partition { it.indexingTimeStats.maxTime.nano > FAST_INDEXER_THRESHOLD_NANO }

  return JsonFileProviderIndexStatistics(
    providerDebugName,
    totalNumberOfFiles,
    JsonTime(totalTime),
    statsPerFileType.sortedByDescending { it.numberOfFiles },
    statsPerIndexer,
    fastIndexers.map { it.indexId }.sorted()
  )
}

typealias PresentableTime = String

private fun TimeMillis.toPresentableTime(): PresentableTime =
  DateFormatUtil.getIso8601Format().format(this)

@Suppress("unused", "used for JSON")
data class JsonProjectIndexingHistoryTimes(
  val startIndexing: PresentableTime,
  val endIndexing: PresentableTime,
  val indexingTime: JsonTime,

  val startPushProperties: PresentableTime,
  val endPushProperties: PresentableTime,
  val pushPropertiesTime: JsonTime,

  val startIndexExtensions: PresentableTime,
  val endIndexExtensions: PresentableTime,
  val indexExtensionsTime: JsonTime,

  val startScanFiles: PresentableTime,
  val endScanFiles: PresentableTime,
  val scanFilesTime: JsonTime
)

fun ProjectIndexingHistory.IndexingTimes.convertToJson(): JsonProjectIndexingHistoryTimes {
  return JsonProjectIndexingHistoryTimes(
    startIndexing.toPresentableTime(),
    endIndexing.toPresentableTime(),
    JsonTime((endIndexing - startIndexing).toNano()),
    startPushProperties.toPresentableTime(),
    endPushProperties.toPresentableTime(),
    JsonTime((endPushProperties - startPushProperties).toNano()),
    startIndexExtensions.toPresentableTime(),
    endIndexExtensions.toPresentableTime(),
    JsonTime((endIndexExtensions - startIndexExtensions).toNano()),
    startScanFiles.toPresentableTime(),
    endScanFiles.toPresentableTime(),
    JsonTime((endScanFiles - startScanFiles).toNano())
  )
}

data class JsonProjectIndexingHistory(
  val projectName: String,
  val indexingTimes: JsonProjectIndexingHistoryTimes,
  val fileProviderStatistics: List<JsonFileProviderIndexStatistics>
)

fun ProjectIndexingHistory.convertToJson(): JsonProjectIndexingHistory =
  JsonProjectIndexingHistory(
    projectName,
    times.convertToJson(),
    providerStatistics.sortedByDescending { it.totalIndexingTime.nano }
  )

data class JsonIndexDiagnosticAppInfo(
  val build: String,
  val buildDate: String,
  val productCode: String,
  val generated: String,
  val os: String,
  val runtime: String
) {
  companion object {
    fun create(): JsonIndexDiagnosticAppInfo {
      val appInfo = ApplicationInfo.getInstance()
      return JsonIndexDiagnosticAppInfo(
        build = appInfo.build.asStringWithoutProductCode(),
        buildDate = ZonedDateTime.ofInstant(
          appInfo.buildDate.toInstant(), ZoneId.systemDefault()
        ).format(DateTimeFormatter.RFC_1123_DATE_TIME),
        productCode = appInfo.build.productCode,
        generated = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME),
        os = SystemInfo.getOsNameAndVersion(),
        runtime = SystemInfo.JAVA_VENDOR + " " + SystemInfo.JAVA_VERSION + " " + SystemInfo.JAVA_RUNTIME_VERSION
      )
    }
  }
}

data class JsonRuntimeInfo(
  val maxMemory: Long,
  val numberOfProcessors: Int,
  val maxNumberOfIndexingThreads: Int,
  val maxSizeOfFileForIntelliSense: Int,
  val maxSizeOfFileForContentLoading: Int
) {
  companion object {
    fun create(): JsonRuntimeInfo {
      val runtime = Runtime.getRuntime()
      return JsonRuntimeInfo(
        runtime.maxMemory(),
        runtime.availableProcessors(),
        UnindexedFilesUpdater.getMaxNumberOfIndexingThreads(),
        PersistentFSConstants.getMaxIntellisenseFileSize(),
        FileUtilRt.LARGE_FOR_CONTENT_LOADING
      )
    }
  }
}

data class JsonIndexDiagnostic(
  val appInfo: JsonIndexDiagnosticAppInfo,
  val runtimeInfo: JsonRuntimeInfo,
  val projectIndexingHistory: JsonProjectIndexingHistory
) {
  companion object {
    fun generateForHistory(projectIndexingHistory: ProjectIndexingHistory): JsonIndexDiagnostic =
      JsonIndexDiagnostic(
        JsonIndexDiagnosticAppInfo.create(),
        JsonRuntimeInfo.create(),
        projectIndexingHistory.convertToJson()
      )
  }
}