// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.text.DateFormatUtil
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private fun TimeNano.toMillis(): TimeMillis = this / 1_000_000

data class JsonTimeStats(
  val minTime: TimeMillis,
  val maxTime: TimeMillis,
  val meanTime: TimeMillis,
  val medianTime: TimeMillis
)

fun MaxNTimeBucket.toTimeStats(): JsonTimeStats =
  JsonTimeStats(
    minTime,
    maxTime,
    meanTime.toLong().toMillis(),
    getMedianOfArray(maxNTimes).toLong().toMillis()
  )

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
  // <total time> = <content loading time> + <indexing time> + <time spent on waiting for other indexing tasks to complete>
  val totalTime: TimeMillis,
  val indexingTimePerFile: JsonTimeStats,
  val contentLoadingTimePerFile: JsonTimeStats,
  val numberOfFilesPerFileType: List<FilesNumberPerFileType>,
  val timesPerFileType: List<TimePerFileType>,
  val timesPerIndexer: List<TimePerIndexer>
) {

  data class TimePerIndexer(val indexId: String, val time: JsonTimeStats)
  data class TimePerFileType(val fileType: String, val time: JsonTimeStats)
  data class FilesNumberPerFileType(val fileType: String, val filesNumber: Int)
}

fun FileProviderIndexStatistics.convertToJson(): JsonFileProviderIndexStatistics =
  JsonFileProviderIndexStatistics(
    providerDebugName,
    totalTime.toMillis(),
    indexingStatistics.indexingTime.toTimeStats(),
    indexingStatistics.contentLoadingTime.toTimeStats(),
    indexingStatistics.numberOfFilesPerFileType
      .map { JsonFileProviderIndexStatistics.FilesNumberPerFileType(it.key, it.value) }
      .sortedByDescending { it.filesNumber }
    ,
    indexingStatistics.timesPerFileType
      .map { JsonFileProviderIndexStatistics.TimePerFileType(it.key, it.value.toTimeStats()) }
      .sortedByDescending { it.time.meanTime }
    ,
    indexingStatistics.timesPerIndexer
      .map { JsonFileProviderIndexStatistics.TimePerIndexer(it.key, it.value.toTimeStats()) }
      .sortedByDescending { it.time.meanTime }
  )

typealias PresentableTime = String

private fun TimeMillis.toPresentableTime(): PresentableTime =
  DateFormatUtil.getIso8601Format().format(this)

@Suppress("unused", "used for JSON")
data class JsonProjectIndexingHistoryTimes(
  val startIndexing: PresentableTime,
  val endIndexing: PresentableTime,
  val indexingTime: TimeMillis,

  val startPushProperties: PresentableTime,
  val endPushProperties: PresentableTime,
  val pushPropertiesTime: TimeMillis,

  val startIndexExtensions: PresentableTime,
  val endIndexExtensions: PresentableTime,
  val indexExtensionsTime: TimeMillis,

  val startScanFiles: PresentableTime,
  val endScanFiles: PresentableTime,
  val scanFilesTime: TimeMillis
)

fun ProjectIndexingHistory.IndexingTimes.convertToJson(): JsonProjectIndexingHistoryTimes {
  return JsonProjectIndexingHistoryTimes(
    startIndexing.toPresentableTime(),
    endIndexing.toPresentableTime(),
    endIndexing - startIndexing,
    startPushProperties.toPresentableTime(),
    endPushProperties.toPresentableTime(),
    endPushProperties - startPushProperties,
    startIndexExtensions.toPresentableTime(),
    endIndexExtensions.toPresentableTime(),
    endIndexExtensions - startIndexExtensions,
    startScanFiles.toPresentableTime(),
    endScanFiles.toPresentableTime(),
    endScanFiles - startScanFiles
  )
}

data class JsonProjectIndexingHistory(
  val projectName: String,
  val indexingTimes: JsonProjectIndexingHistoryTimes,
  val fileProviderStatistics: List<JsonFileProviderIndexStatistics>
)

fun ProjectIndexingHistory.convertToJson(): JsonProjectIndexingHistory {
  return JsonProjectIndexingHistory(
    projectName,
    times.convertToJson(),
    providerStatistics.sortedByDescending { it.indexingTimePerFile.meanTime }
  )
}

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