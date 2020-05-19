// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

class IndexingJobStatistics {
  private val timeBucketSize = 128

  val timesPerIndexer: Map<String /* ID.name() */, MaxNTimeBucket>
    get() = _timesPerIndexer

  val timesPerFileType: Map<String /* File type name */, MaxNTimeBucket>
    get() = _timesPerFileType

  val numberOfFilesPerFileType: Map<String /* File type name */, Int>
    get() = _numberOfFilesPerFileType

  val contentLoadingTime: MaxNTimeBucket
    get() = _contentLoadingTime

  val indexingTime: MaxNTimeBucket
    get() = _indexingTime

  private val _timesPerIndexer = hashMapOf<String, MaxNTimeBucket>()
  private val _timesPerFileType = hashMapOf<String, MaxNTimeBucket>()
  private val _numberOfFilesPerFileType = hashMapOf<String, Int>()
  private val _contentLoadingTime = MaxNTimeBucket(timeBucketSize, 0)
  private val _indexingTime = MaxNTimeBucket(timeBucketSize, 0)

  @Synchronized
  fun addFileStatistics(fileStatistics: FileIndexingStatistics) {
    fileStatistics.perIndexerTimes.forEach { (indexId, time) ->
      _timesPerIndexer.getOrPut(indexId.name) { MaxNTimeBucket(timeBucketSize, time) }.addTime(time)
    }
    val fileTypeName = fileStatistics.fileType.name
    _numberOfFilesPerFileType.compute(fileTypeName) { _, currentNumber -> (currentNumber ?: 0) + 1 }
    _timesPerFileType.computeIfAbsent(fileTypeName) { MaxNTimeBucket(timeBucketSize, fileStatistics.totalTime) }.addTime(fileStatistics.totalTime)
  }

  @Synchronized
  fun addIndexingTime(nanoTime: TimeNano) {
    _indexingTime.addTime(nanoTime)
  }

  @Synchronized
  fun addContentLoadingTime(nanoTime: TimeNano) {
    _contentLoadingTime.addTime(nanoTime)
  }
}