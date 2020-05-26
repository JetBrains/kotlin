// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

class IndexingJobStatistics {
  private val timeBucketSize = 128

  val timesPerIndexer: Map<String /* ID.name() */, MaxNTimeBucket>
    get() = _timesPerIndexer

  val indexingTimesPerFileType: Map<String /* File type name */, MaxNTimeBucket>
    get() = _indexingTimesPerFileType

  val contentLoadingTimesPerFileType: Map<String /* File type name */, MaxNTimeBucket>
    get() = _contentLoadingTimesPerFileType

  val numberOfFilesPerFileType: Map<String /* File type name */, Int>
    get() = _numberOfFilesPerFileType

  private val _timesPerIndexer = hashMapOf<String, MaxNTimeBucket>()
  private val _indexingTimesPerFileType = hashMapOf<String, MaxNTimeBucket>()
  private val _contentLoadingTimesPerFileType = hashMapOf<String, MaxNTimeBucket>()
  private val _numberOfFilesPerFileType = hashMapOf<String, Int>()

  @Synchronized
  fun addFileStatistics(fileStatistics: FileIndexingStatistics, contentLoadingTime: Long) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      _timesPerIndexer.getOrPut(indexId.name) { MaxNTimeBucket(timeBucketSize) }.addTime(time)
    }
    val fileTypeName = fileStatistics.fileType.name
    _numberOfFilesPerFileType.compute(fileTypeName) { _, currentNumber -> (currentNumber ?: 0) + 1 }
    _indexingTimesPerFileType.computeIfAbsent(fileTypeName) { MaxNTimeBucket(timeBucketSize) }.addTime(fileStatistics.indexingTime)
    _contentLoadingTimesPerFileType.computeIfAbsent(fileTypeName) { MaxNTimeBucket(timeBucketSize) }.addTime(contentLoadingTime)
  }
}