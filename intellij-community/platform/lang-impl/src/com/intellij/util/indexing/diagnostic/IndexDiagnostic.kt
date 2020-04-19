// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

typealias TimeMillis = Long
typealias TimeNano = Long

data class ProjectIndexingHistory(val projectName: String) {
  val times: IndexingTimes = IndexingTimes()

  val providerStatistics: MutableList<FileProviderIndexStatistics> = arrayListOf()

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