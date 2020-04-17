// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

data class ProjectIndexingHistory(val projectName: String) {
  val times: IndexingTimes = IndexingTimes()

  val providerStatistics: MutableList<FileProviderIndexStatistics> = arrayListOf()

  data class IndexingTimes(
    var startIndexing: Long = 0,
    var endIndexing: Long = 0,

    var startPushProperties: Long = 0,
    var endPushProperties: Long = 0,

    var startIndexExtensions: Long = 0,
    var endIndexExtensions: Long = 0,

    var startScanFiles: Long = 0,
    var endScanFiles: Long = 0
  )
}