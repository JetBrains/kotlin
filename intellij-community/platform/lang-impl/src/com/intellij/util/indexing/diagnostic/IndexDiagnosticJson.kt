// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.DateFormatUtil
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class JsonPerThreadTime(
  val perThreadTimes: List<TimeNano>,
  val totalCpuTime: TimeNano
)

fun PerThreadTime.toJsonPerThreadTime(): JsonPerThreadTime =
  JsonPerThreadTime(
    threadIdToTime.values.sortedDescending(),
    threadIdToTime.values.sum()
  )

data class JsonFileProviderIndexStatistics(
  val indexableFilesProviderDebugName: String,
  // <total time> = <content loading time> + <indexing time> + <time spent on waiting for other indexing tasks to complete>
  val totalTime: TimeNano,
  val indexingTime: JsonPerThreadTime,
  val contentLoadingTime: JsonPerThreadTime,
  val numberOfFilesPerFileType: List<FilesNumberPerFileType>,
  val timesPerFileType: List<TimePerFileType>,
  val timesPerIndexer: List<TimePerIndexer>
) {

  data class TimePerIndexer(val indexId: String, val time: JsonPerThreadTime)
  data class TimePerFileType(val fileType: String, val time: JsonPerThreadTime)
  data class FilesNumberPerFileType(val fileType: String, val filesNumber: Int)
}

fun FileProviderIndexStatistics.convertToJson(): JsonFileProviderIndexStatistics =
  JsonFileProviderIndexStatistics(
    providerDebugName,
    totalTime,
    indexingStatistics.indexingTime.toJsonPerThreadTime(),
    indexingStatistics.contentLoadingTime.toJsonPerThreadTime(),
    indexingStatistics.numberOfFilesPerFileType
      .map { JsonFileProviderIndexStatistics.FilesNumberPerFileType(it.key, it.value) }
      .sortedByDescending { it.filesNumber }
    ,
    indexingStatistics.timesPerFileType
      .map { JsonFileProviderIndexStatistics.TimePerFileType(it.key, it.value.toJsonPerThreadTime()) }
      .sortedByDescending { it.time.totalCpuTime }
    ,
    indexingStatistics.timesPerIndexer
      .map { JsonFileProviderIndexStatistics.TimePerIndexer(it.key, it.value.toJsonPerThreadTime()) }
      .sortedByDescending { it.time.totalCpuTime }
  )

@Suppress("unused", "used for JSON")
data class JsonProjectIndexingHistoryTimes(
  val startIndexing: TimeMillis,
  val endIndexing: TimeMillis,

  val startPushProperties: TimeMillis,
  val endPushProperties: TimeMillis,

  val startIndexExtensions: TimeMillis,
  val endIndexExtensions: TimeMillis,

  val startScanFiles: TimeMillis,
  val endScanFiles: TimeMillis
) {
  val presentableStartIndexingTime: String get() = DateFormatUtil.formatTimeWithSeconds(startIndexing)
  val indexingTime: TimeMillis get() = endIndexing - startIndexing
  val pushPropertiesTime: TimeMillis get() = endPushProperties - startPushProperties
  val indexExtensionsTime: TimeMillis get() = endIndexExtensions - startIndexExtensions
  val scanFilesTime: TimeMillis get() = endScanFiles - startScanFiles
}

fun ProjectIndexingHistory.IndexingTimes.convertToJson(): JsonProjectIndexingHistoryTimes =
  JsonProjectIndexingHistoryTimes(
    startIndexing,
    endIndexing,
    startPushProperties,
    endPushProperties,
    startIndexExtensions,
    endIndexExtensions,
    startScanFiles,
    endScanFiles
  )

data class JsonProjectIndexingHistory(
  val projectName: String,
  val indexingTimes: JsonProjectIndexingHistoryTimes,
  val providerStatistics: List<JsonFileProviderIndexStatistics>
)

fun ProjectIndexingHistory.convertToJson(): JsonProjectIndexingHistory {
  return JsonProjectIndexingHistory(
    projectName,
    times.convertToJson(),
    providerStatistics
      .map { it.convertToJson() }
      .sortedByDescending { it.indexingTime.totalCpuTime }
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

data class JsonIndexDiagnostic(
  val appInfo: JsonIndexDiagnosticAppInfo,
  val projectIndexingHistory: JsonProjectIndexingHistory
) {
  companion object {
    fun generateForHistory(projectIndexingHistory: ProjectIndexingHistory): JsonIndexDiagnostic =
      JsonIndexDiagnostic(
        JsonIndexDiagnosticAppInfo.create(),
        projectIndexingHistory.convertToJson()
      )
  }
}