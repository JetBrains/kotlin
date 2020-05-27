// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.internal;

import com.intellij.build.TasksViewManager;
import com.intellij.build.events.BuildEvent;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

/**
 * @author Vladislav.Soroka
 */
@TestOnly
public class DummyTasksViewManager extends TasksViewManager {
  public DummyTasksViewManager(Project project) {
    super(project);
  }

  @NotNull
  @Override
  protected String getViewName() {
    return LangBundle.message("tasks.view.title");
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {}
}
