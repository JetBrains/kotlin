// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.compiler.options;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface ExcludedEntriesListener {
  /**
   * Compiler's excluded entries modification notification.
   */
  Topic<ExcludedEntriesListener> TOPIC = new Topic<>(ExcludedEntriesListener.class, Topic.BroadcastDirection.NONE);

  default void onEntryAdded(@NotNull ExcludeEntryDescription description) {}

  default void onEntryRemoved(@NotNull ExcludeEntryDescription description) {}
}