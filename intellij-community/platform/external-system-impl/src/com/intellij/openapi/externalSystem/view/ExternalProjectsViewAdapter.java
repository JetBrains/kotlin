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
public class ExternalProjectsViewAdapter implements ExternalProjectsView {
  @NotNull
  private final ExternalProjectsView delegate;

  public ExternalProjectsViewAdapter(@NotNull ExternalProjectsView delegate) {
    this.delegate = delegate;
  }

  @Override
  public ExternalSystemUiAware getUiAware() {
    return delegate.getUiAware();
  }

  @Override
  @Nullable
  public ExternalProjectsStructure getStructure() {
    return delegate.getStructure();
  }

  @Override
  public ExternalSystemShortcutsManager getShortcutsManager() {
    return delegate.getShortcutsManager();
  }

  @Override
  public ExternalSystemTaskActivator getTaskActivator() {
    return delegate.getTaskActivator();
  }

  @Override
  public void updateUpTo(ExternalSystemNode node) {
    delegate.updateUpTo(node);
  }

  @Override
  public List<ExternalSystemNode<?>> createNodes(@NotNull ExternalProjectsView externalProjectsView,
                                                 @Nullable ExternalSystemNode<?> parent,
                                                 @NotNull DataNode<?> dataNode) {
    return delegate.createNodes(externalProjectsView, parent, dataNode);
  }

  @Override
  public Project getProject() {
    return delegate.getProject();
  }

  @Override
  public boolean showInheritedTasks() {
    return delegate.showInheritedTasks();
  }

  @Override
  public boolean getGroupTasks() {
    return delegate.getGroupTasks();
  }

  @Override
  public boolean getGroupModules() {
    return delegate.getGroupModules();
  }

  @Override
  public boolean useTasksNode() {
    return delegate.useTasksNode();
  }

  @Override
  public ProjectSystemId getSystemId() {
    return delegate.getSystemId();
  }

  @Override
  public void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
    delegate.handleDoubleClickOrEnter(node, actionId, inputEvent);
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    delegate.addListener(listener);
  }

  @Override
  public boolean getShowIgnored() {
    return delegate.getShowIgnored();
  }

  @Override
  public String getDisplayName(DataNode node) {
    return delegate.getDisplayName(node);
  }
}
