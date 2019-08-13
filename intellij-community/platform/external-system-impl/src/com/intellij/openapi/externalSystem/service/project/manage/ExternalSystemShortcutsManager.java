// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapManagerListener;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemShortcutsManager implements Disposable {

  private static final String ACTION_ID_PREFIX = "ExternalSystem_";
  @NotNull
  private final Project myProject;
  private final List<Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public ExternalSystemShortcutsManager(@NotNull Project project) {
    myProject = project;
  }

  public void init() {
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(KeymapManagerListener.TOPIC, new KeymapManagerListener() {
      @Override
      public void activeKeymapChanged(Keymap keymap) {
        fireShortcutsUpdated();
      }

      @Override
      public void shortcutChanged(@NotNull Keymap keymap, @NotNull String actionId) {
        fireShortcutsUpdated();
      }
    });
  }

  @NotNull
  public String getActionId(@Nullable String projectPath, @Nullable String taskName) {
    StringBuilder result = new StringBuilder(ACTION_ID_PREFIX);
    result.append(myProject.getLocationHash());

    if (projectPath != null) {
      String portablePath = FileUtil.toSystemIndependentName(projectPath);
      File file = new File(portablePath);
      result.append(file.isFile() && file.getParentFile() != null ? file.getParentFile().getName() : file.getName());
      result.append(Integer.toHexString(portablePath.hashCode()));

      if (taskName != null) result.append(taskName);
    }

    return result.toString();
  }

  public String getDescription(@Nullable String projectPath, @Nullable String taskName) {
    Shortcut[] shortcuts = getShortcuts(projectPath, taskName);
    if (shortcuts.length == 0) return "";
    return KeymapUtil.getShortcutsText(shortcuts);
  }

  public boolean hasShortcuts(@Nullable String projectPath, @Nullable String taskName) {
    return getShortcuts(projectPath, taskName).length > 0;
  }

  public boolean hasShortcuts(@NotNull String actionId) {
    Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
    return activeKeymap.getShortcuts(actionId).length > 0;
  }

  @NotNull
  private Shortcut[] getShortcuts(@Nullable String projectPath, @Nullable String taskName) {
    String actionId = getActionId(projectPath, taskName);
    Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
    return activeKeymap.getShortcuts(actionId);
  }

  private void fireShortcutsUpdated() {
    for (Listener listener : myListeners) {
      listener.shortcutsUpdated();
    }
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  @FunctionalInterface
  public interface Listener {
    void shortcutsUpdated();
  }

  void scheduleKeymapUpdate(@NotNull Collection<? extends DataNode<TaskData>> taskData) {
    ExternalSystemKeymapExtension.updateActions(myProject, taskData);
  }

  void scheduleRunConfigurationKeymapUpdate(@NotNull ProjectSystemId externalSystemId) {
    ExternalSystemKeymapExtension.updateRunConfigurationActions(myProject, externalSystemId);
  }

  @Override
  public void dispose() {
    ExternalSystemKeymapExtension.clearActions(this);
  }
}
