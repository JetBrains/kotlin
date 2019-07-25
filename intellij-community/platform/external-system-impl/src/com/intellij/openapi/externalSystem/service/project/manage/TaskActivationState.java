/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Tag("activation")
public class TaskActivationState {
  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "before_run")
  public List<String> beforeRunTasks = new ArrayList<>();

  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "before_sync")
  public List<String> beforeSyncTasks = new ArrayList<>();

  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "after_sync")
  public List<String> afterSyncTasks = new ArrayList<>();

  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "before_compile")
  public List<String> beforeCompileTasks = new ArrayList<>();

  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "after_compile")
  public List<String> afterCompileTasks = new ArrayList<>();

  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "after_rebuild")
  public List<String> afterRebuildTask = new ArrayList<>();

  @XCollection(elementName = "task", valueAttributeName = "name", propertyElementName = "before_rebuild")
  public List<String> beforeRebuildTask = new ArrayList<>();

  public boolean isEmpty() {
    for (ExternalSystemTaskActivator.Phase phase : ExternalSystemTaskActivator.Phase.values()) {
      if (!getTasks(phase).isEmpty()) return false;
    }
    return true;
  }

  @NotNull
  public List<String> getTasks(@NotNull ExternalSystemTaskActivator.Phase phase) {
    switch (phase) {
      case AFTER_COMPILE:
        return afterCompileTasks;
      case BEFORE_COMPILE:
        return beforeCompileTasks;
      case AFTER_SYNC:
        return afterSyncTasks;
      case BEFORE_RUN:
        return beforeRunTasks;
      case BEFORE_SYNC:
        return beforeSyncTasks;
      case AFTER_REBUILD:
        return afterRebuildTask;
      case BEFORE_REBUILD:
        return beforeRebuildTask;
      default:
        throw new IllegalArgumentException("Unknown task activation phase: " + phase);
    }
  }
}
