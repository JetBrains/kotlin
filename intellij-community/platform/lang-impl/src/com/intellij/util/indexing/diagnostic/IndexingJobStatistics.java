// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IndexingJobStatistics {
  final ConcurrentMap<String, PerThreadTime> timesPerIndexer = new ConcurrentHashMap<>();
  final ConcurrentMap<String, PerThreadTime> timesPerFileType = new ConcurrentHashMap<>();
  final ConcurrentMap<String, Integer> numberOfFilesPerFileType = new ConcurrentHashMap<>();
  final PerThreadTime contentLoadingTime = new PerThreadTime();
  final PerThreadTime indexingTime = new PerThreadTime();

  public void addFileStatistics(@NotNull FileIndexingStatistics fileStatistics, @NotNull FileType fileType) {
    fileStatistics.perIndexerTimes.forEach((indexId, time) -> {
      timesPerFileType
        .computeIfAbsent(indexId.getName(), (__) -> new PerThreadTime())
        .addTimeSpentInCurrentThread(time);
    });
    String fileTypeName = fileType.getName();
    numberOfFilesPerFileType.compute(fileTypeName, (__, currentNumber) -> (currentNumber != null ? currentNumber : 0) + 1);
    timesPerFileType
      .computeIfAbsent(fileTypeName, (__) -> new PerThreadTime())
      .addTimeSpentInCurrentThread(fileStatistics.totalTime);
  }

  public void addIndexingTime(long ms) {
    indexingTime.addTimeSpentInCurrentThread(ms);
  }

  public void addContentLoadingTime(long ms) {
    contentLoadingTime.addTimeSpentInCurrentThread(ms);
  }
}
