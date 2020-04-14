// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ProjectViewListener {
  Topic<ProjectViewListener> TOPIC = new Topic<>(ProjectViewListener.class);

  /**
   * @param current  a pane that is currently shown
   * @param previous a pane that was previously shown or {@code null} for the first call
   */
  default void paneShown(@NotNull AbstractProjectViewPane current, @Nullable AbstractProjectViewPane previous) {
  }
}
