// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class StatsCollectorBundle extends AbstractBundle {
  private static final String STATS_COLLECTOR_BUNDLE = "messages.StatsCollectorBundle";

  public static String message(@NotNull @PropertyKey(resourceBundle = STATS_COLLECTOR_BUNDLE) String key, @NotNull Object... params) {
    return ourInstance.getMessage(key, params);
  }

  private static final AbstractBundle ourInstance = new StatsCollectorBundle();

  protected StatsCollectorBundle() {
    super(STATS_COLLECTOR_BUNDLE);
  }
}
