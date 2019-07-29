// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public final class PerformanceTrace implements Serializable {
  public static final Key<PerformanceTrace> TRACE_NODE_KEY = Key.create(PerformanceTrace.class, ExternalSystemConstants.UNORDERED + 1);

  private final Map<String, Long> performanceData = new ConcurrentSkipListMap<>();

  public void logPerformance(@NotNull String key, long millis) {
    performanceData.put(key, millis);
  }

  @NotNull
  public Map<String, Long> getPerformanceTrace() {
    return performanceData;
  }

  public void addTrace(@NotNull Map<String, Long> trace) {
    performanceData.putAll(trace);
  }
}
