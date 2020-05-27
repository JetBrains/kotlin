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
      gen.writeString(value.presentableDuration())
    }
  }

  fun presentableDuration(): String =
    if (nano < TimeUnit.MILLISECONDS.toNanos(1)) {
      "< 1 ms"
    }
    else {
      StringUtil.formatDuration(nano.toMillis())
    }
}

@JsonSerialize(using = JsonPercentages.Companion::class)
data class JsonPercentages(val percentages: Double) {
  companion object : JsonSerializer<JsonPercentages>() {
    override fun serialize(value: JsonPercentages, gen: JsonGenerator, serializers: SerializerProvider?) {
      gen.writeString(value.presentablePercentages())
    }
  }

  fun presentablePercentages(): String =
    if (percentages < 0.01) {
      "< 1%"
    }
    else {
      "${String.format("%.1f", percentages * 100)}%"
    }
}

@JsonSerialize(using = JsonFileSize.Companion::class)
data class JsonFileSize(val bytes: BytesNumber) {
  companion object : JsonSerializer<JsonFileSize>() {
    override fun serialize(value: JsonFileSize, gen: JsonGenerator, serializers: SerializerProvider?) {
      gen.writeString(value.presentableSize())
    }
  }

  fun presentableSize(): String = StringUtil.formatFileSize(bytes)
}

@JsonSerialize(using = JsonProcessingSpeed.Companion::class)
data class JsonProcessingSpeed(val totalBytes: BytesNumber, val totalTime: TimeNano) {
  companion object : JsonSerializer<JsonProcessingSpeed>() {
    override fun serialize(value: JsonProcessingSpeed, gen: JsonGenerator, serializers: SerializerProvider?) {
      gen.writeString(value.presentableSpeed())
    }
  }

  fun presentableSpeed(): String {
    if (totalTime == 0L) {
      return "0 B/s"
    }
    val bytesPerSecond = (totalBytes.toDouble() * TimeUnit.SECONDS.toNanos(1).toDouble() / totalTime).toLong()
    return StringUtil.formatFileSize(bytesPerSecond) + "/s"
  }
}

data class JsonFileProviderIndexStatistics(
  val providerName: String,
  val totalNumberOfFiles: Int,
  val totalIndexingTime: JsonTime,
  val statsPerFileType: List<JsonStatsPerFileType>,
  val statsPerIndexer: List<JsonStatsPerIndexer>,
  val fastIndexers: List<String /* Index ID */>
) {

  data class JsonStatsPerFileType(
    val fileType: String,
    val numberOfFiles: Int,
    val totalFilesSize: JsonFileSize,
    val partOfTotalIndexingTime: JsonPercentages,
    val partOfTotalContentLoadingTime: JsonPercentages,
    val biggestContributors: List<JsonIndexedFileStat>
  )

  data class JsonStatsPerIndexer(
    val indexId: String,
    val partOfTotalIndexingTime: JsonPercentages
  )
}

fun FileProviderIndexStatistics.convertToJson(): JsonFileProviderIndexStatistics {
  val statsPerFileType = aggregateStatsPerFileType()
  val allStatsPerIndexer = aggregateStatsPerIndexer()
  val (statsPerIndexer, fastIndexers) = allStatsPerIndexer.partition { it.partOfTotalIndexingTime.percentages > 0.01 }

  return JsonFileProviderIndexStatistics(
    providerDebugName,
    numberOfFiles,
    JsonTime(totalTime),
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
        JsonPercentages(calculatePart(stats.indexingTime.sumTime, totalIndexingTimePerFileType)),
        JsonPercentages(calculatePart(stats.contentLoadingTime.sumTime, totalContentLoadingTimePerFileType)),
        stats.biggestContributors.biggestIndexedFiles.map { it.toJson() }.sortedByDescending { it.indexingTime.nano }
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
        JsonPercentages(calculatePart(stats.indexingTime.sumTime, totalIndexingTimePerIndexer))
      )
    }
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

fun ProjectIndexingHistory.IndexingTimes.convertToJson() =
  JsonProjectIndexingHistoryTimes(
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

data class JsonIndexedFileStat(
  val fileName: String,
  val fileType: String,
  val fileSize: JsonFileSize,
  val indexingTime: JsonTime
)

data class JsonProjectIndexingHistory(
  val projectName: String,
  val totalNumberOfFiles: Int,
  val indexingTimes: JsonProjectIndexingHistoryTimes,
  val totalStatsPerFileType: List<JsonStatsPerFileType>,
  val totalStatsPerIndexer: List<JsonStatsPerIndexer>,
  val fileProviderStatistics: List<JsonFileProviderIndexStatistics>
) {
  data class JsonStatsPerFileType(
    val fileType: String,
    val partOfTotalIndexingTime: JsonPercentages,
    val partOfTotalContentLoadingTime: JsonPercentages,
    val totalNumberOfFiles: Int,
    val totalFilesSize: JsonFileSize,
    val indexingSpeed: JsonProcessingSpeed,
    val biggestContributors: List<JsonIndexedFileStat>
  )

  data class JsonStatsPerIndexer(
    val indexId: String,
    val partOfTotalIndexingTime: JsonPercentages,
    val totalNumberOfFiles: Int,
    val totalFilesSize: JsonFileSize,
    val indexingSpeed: JsonProcessingSpeed
  )
}

private fun calculatePart(part: Long, total: Long): Double =
  if (total == 0L) {
    1.0
  }
  else {
    part.toDouble() / total
  }

fun ProjectIndexingHistory.convertToJson() =
  JsonProjectIndexingHistory(
    projectName,
    aggregateTotalNumberOfFiles(),
    times.convertToJson(),
    aggregateStatsPerFileType().sortedByDescending { it.partOfTotalIndexingTime.percentages },
    aggregateStatsPerIndexer().sortedByDescending { it.partOfTotalIndexingTime.percentages },
    providerStatistics.sortedByDescending { it.totalIndexingTime.nano }
  )

private fun ProjectIndexingHistory.aggregateTotalNumberOfFiles() =
  providerStatistics.map { it.totalNumberOfFiles }.sum()

private fun ProjectIndexingHistory.aggregateStatsPerFileType(): List<JsonProjectIndexingHistory.JsonStatsPerFileType> {
  val totalIndexingTime = totalStatsPerFileType.values.map { it.totalIndexingTimeInAllThreads }.sum()
  val fileTypeToIndexingTimePart = totalStatsPerFileType.mapValues {
    calculatePart(it.value.totalIndexingTimeInAllThreads, totalIndexingTime)
  }

  @Suppress("DuplicatedCode")
  val totalContentLoadingTime = totalStatsPerFileType.values.map { it.totalContentLoadingTimeInAllThreads }.sum()
  val fileTypeToContentLoadingTimePart = totalStatsPerFileType.mapValues {
    calculatePart(it.value.totalContentLoadingTimeInAllThreads, totalContentLoadingTime)
  }

  val fileTypeToProcessingSpeed = totalStatsPerFileType.mapValues {
    JsonProcessingSpeed(it.value.totalBytes, it.value.totalIndexingTimeInAllThreads)
  }

  return totalStatsPerFileType.map { (fileType, stats) ->
    JsonProjectIndexingHistory.JsonStatsPerFileType(
      fileType,
      JsonPercentages(fileTypeToIndexingTimePart.getValue(fileType)),
      JsonPercentages(fileTypeToContentLoadingTimePart.getValue(fileType)),
      stats.totalNumberOfFiles,
      JsonFileSize(stats.totalBytes),
      fileTypeToProcessingSpeed.getValue(fileType),
      stats.biggestContributors.biggestIndexedFiles.map { it.toJson() }.sortedByDescending { it.indexingTime.nano }
    )
  }
}

private fun IndexedFileStat.toJson() =
  JsonIndexedFileStat(
    fileName,
    fileType,
    JsonFileSize(fileSize),
    JsonTime(indexingTime)
  )

private fun ProjectIndexingHistory.aggregateStatsPerIndexer(): List<JsonProjectIndexingHistory.JsonStatsPerIndexer> {
  val totalIndexingTime = totalStatsPerIndexer.values.map { it.totalIndexingTimeInAllThreads }.sum()
  val indexIdToIndexingTimePart = totalStatsPerIndexer.mapValues {
    calculatePart(it.value.totalIndexingTimeInAllThreads, totalIndexingTime)
  }

  val indexIdToProcessingSpeed = totalStatsPerIndexer.mapValues {
    JsonProcessingSpeed(it.value.totalBytes, it.value.totalIndexingTimeInAllThreads)
  }

  return totalStatsPerIndexer.map { (indexId, stats) ->
    JsonProjectIndexingHistory.JsonStatsPerIndexer(
      indexId,
      JsonPercentages(indexIdToIndexingTimePart.getValue(indexId)),
      stats.totalNumberOfFiles,
      JsonFileSize(stats.totalBytes),
      indexIdToProcessingSpeed.getValue(indexId)
    )
  }
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