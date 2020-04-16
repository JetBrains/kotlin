// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IndexVersionRegistrationSink {

  private final Map<ID<?, ?>, IndexingStamp.IndexVersionDiff> indexVersionDiffs = new ConcurrentHashMap<>();

  public boolean hasChangedIndexes() {
    return ContainerUtil.find(indexVersionDiffs.values(), diff -> isRebuildRequired(diff)) != null;
  }

  public @NotNull String changedIndices() {
    return buildString(diff -> isRebuildRequired(diff));
  }

  public void logChangedAndFullyBuiltIndices(@NotNull Logger log,
                                             @NotNull String changedIndicesLogMessage,
                                             @NotNull String fullyBuiltIndicesLogMessage) {
    String changedIndices = changedIndices();
    if (!changedIndices.isEmpty()) {
      log.info(changedIndicesLogMessage + changedIndices);
    }
    String fullyBuiltIndices = initiallyBuiltIndices();
    if (!fullyBuiltIndices.isEmpty()) {
      log.info(fullyBuiltIndicesLogMessage + fullyBuiltIndices);
    }
  }

  private @NotNull String buildString(@NotNull Predicate<IndexingStamp.IndexVersionDiff> condition) {
    return indexVersionDiffs
      .entrySet()
      .stream()
      .filter(e -> condition.apply(e.getValue()))
      .map(e -> e.getKey().getName() + e.getValue().getLogText())
      .collect(Collectors.joining(","));
  }

  private String initiallyBuiltIndices() {
    return buildString(diff -> diff instanceof IndexingStamp.IndexVersionDiff.InitialBuild);
  }

  public <K, V> void setIndexVersionDiff(@NotNull ID<K, V> name, @NotNull IndexingStamp.IndexVersionDiff diff) {
    indexVersionDiffs.put(name, diff);
  }

  private static boolean isRebuildRequired(@NotNull IndexingStamp.IndexVersionDiff diff) {
    return diff instanceof IndexingStamp.IndexVersionDiff.CorruptedRebuild ||
           diff instanceof IndexingStamp.IndexVersionDiff.VersionChanged;
  }
}