// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui.layout.impl;

import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockContainerFactory;
import com.intellij.ui.docking.DockableContent;
import org.jetbrains.annotations.NotNull;

public final class DockableGridContainerFactory implements DockContainerFactory {
  public static final String TYPE = "runner-grid";

  @Override
  public DockContainer createContainer(@NotNull DockableContent content) {
    RunnerContentUi.DockableGrid dockableGrid = (RunnerContentUi.DockableGrid)content;
    return new RunnerContentUi(dockableGrid.getRunnerUi(), dockableGrid.getOriginalRunnerUi(), dockableGrid.getWindow());
  }
}
