// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.task.ProjectTask;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public final class ProjectTaskList extends SmartList<ProjectTask> implements ProjectTask {
  public ProjectTaskList() {
  }

  public ProjectTaskList(@NotNull Collection<? extends ProjectTask> c) {
    super(c);
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return toString();
  }

  @NotNull
  public static ProjectTaskList asList(ProjectTask... tasks) {
    return new ProjectTaskList(Arrays.asList(tasks));
  }
}
