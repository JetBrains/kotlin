// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class PerformanceTrace {
  public static final Key<PerformanceTrace> TRACE_NODE_KEY = Key.create(PerformanceTrace.class, ExternalSystemConstants.UNORDERED + 1);

  private final Map<String, Long> myPerformanceData = new LinkedHashMap<>();

  public void logPerformance(@NotNull final String key, long millis) {
    myPerformanceData.put(key, millis);
  }

  public Map<String, Long> getPerformanceTrace() {
    return myPerformanceData;
  }

  public void addTrace(@NotNull final Map<String, Long> trace) {
    myPerformanceData.putAll(trace);
  }
}
