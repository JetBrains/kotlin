// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public class IndicesRegistrationResult {
  private final Map<ID<?, ?>, IndexState> updatedIndices = ContainerUtil.newConcurrentMap();

  @NotNull
  public String changedIndices() {
    return buildAffectedIndicesString(IndexState.VERSION_CHANGED);
  }

  @NotNull
  private String buildAffectedIndicesString(IndexState state) {
    return updatedIndices.keySet().stream().filter(id -> updatedIndices.get(id) == state).map(id -> id.getName()).collect(
      Collectors.joining(","));
  }

  private String fullyBuiltIndices() {
    return buildAffectedIndicesString(IndexState.INITIAL_BUILD);
  }

  public void logChangedAndFullyBuiltIndices(@NotNull Logger log, @NotNull String changedIndicesLogMessage,
                                             @NotNull String fullyBuiltIndicesLogMessage) {
    String changedIndices = changedIndices();
    if (!changedIndices.isEmpty()) {
      log.info(changedIndicesLogMessage + changedIndices);
    }
    String fullyBuiltIndices = fullyBuiltIndices();
    if (!fullyBuiltIndices.isEmpty()) {
      log.info(fullyBuiltIndicesLogMessage + fullyBuiltIndices);
    }
  }

  private enum IndexState {
    VERSION_CHANGED, INITIAL_BUILD
  }

  public void registerIndexAsUptoDate(@NotNull ID<?, ?> index) {
    updatedIndices.remove(index);
  }

  public void registerIndexAsInitiallyBuilt(@NotNull ID<?, ?> index) {
    updatedIndices.put(index, IndexState.INITIAL_BUILD);
  }

  public void registerIndexAsChanged(@NotNull ID<?, ?> index) {
    updatedIndices.put(index, IndexState.VERSION_CHANGED);
  }
}