// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings;

import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

public class CompletionStatsCollectorSettings {
  @NotNull
  public static CompletionStatsCollectorSettings getInstance() {
    return Holder.INSTANCE;
  }

  public boolean isCompletionLogsSendAllowed() {
    return Registry.is("completion.stats.send.logs");
  }

  public boolean isRankingEnabled() {
    return Registry.is("completion.stats.enable.ml.ranking");
  }

  private static class Holder {
    private static final CompletionStatsCollectorSettings INSTANCE = new CompletionStatsCollectorSettings();
  }
}
