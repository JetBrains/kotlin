// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.internal;

import com.intellij.build.TasksViewManager;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FinishBuildEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class DummyTasksViewManager extends TasksViewManager {
  public DummyTasksViewManager(Project project) {
    super(project);
  }

  @NotNull
  @Override
  protected String getViewName() {
    return "Tasks";
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    if(event instanceof FinishBuildEvent) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println(event.getMessage());
    }
  }
}
