/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.ui.layout.impl;

import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockContainerFactory;
import com.intellij.ui.docking.DockableContent;

/**
 * @author Dennis.Ushakov
 */
public class DockableGridContainerFactory implements DockContainerFactory {
  public static final String TYPE = "runner-grid";

  @Override
  public DockContainer createContainer(DockableContent content) {
    final RunnerContentUi.DockableGrid dockableGrid = (RunnerContentUi.DockableGrid)content;
    return new RunnerContentUi(dockableGrid.getRunnerUi(), dockableGrid.getOriginalRunnerUi(), dockableGrid.getWindow());
  }

  @Override
  public void dispose() {}
}
