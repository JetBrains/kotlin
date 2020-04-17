// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic;

import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class FileIndexingStatistics {
  final long totalTime;
  final Map<ID<?, ?>, Long> perIndexerTimes;

  public FileIndexingStatistics(long totalTime, @NotNull Map<ID<?, ?>, Long> perIndexerTimes) {
    this.totalTime = totalTime;
    this.perIndexerTimes = perIndexerTimes;
  }
}
