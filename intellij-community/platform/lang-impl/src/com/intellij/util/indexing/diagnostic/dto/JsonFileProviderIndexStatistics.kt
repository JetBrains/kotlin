// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

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