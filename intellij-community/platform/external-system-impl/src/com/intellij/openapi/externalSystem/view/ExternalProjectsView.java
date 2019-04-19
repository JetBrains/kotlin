/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.view;

import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.InputEvent;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalProjectsView {
  ExternalSystemUiAware getUiAware();

  @Nullable
  ExternalProjectsStructure getStructure();

  ExternalSystemShortcutsManager getShortcutsManager();

  ExternalSystemTaskActivator getTaskActivator();

  void updateUpTo(ExternalSystemNode node);

  List<ExternalSystemNode<?>> createNodes(@NotNull ExternalProjectsView externalProjectsView, @Nullable ExternalSystemNode<?> parent, @NotNull DataNode<?> dataNode);

  Project getProject();

  boolean showInheritedTasks();

  boolean getGroupTasks();

  boolean getGroupModules();

  boolean useTasksNode();

  ProjectSystemId getSystemId();

  void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent);

  void addListener(@NotNull ExternalProjectsView.Listener listener);

  boolean getShowIgnored();

  @Nullable
  String getDisplayName(@Nullable DataNode node);

  interface Listener {
    void onDoubleClickOrEnter(@NotNull ExternalSystemNode node, InputEvent inputEvent);
  }
}
