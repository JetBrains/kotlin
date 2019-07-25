// Copyright 2000-2017 JetBrains s.r.o.
// Use of this source code is governed by the Apache 2.0 license that can be
// found in the LICENSE file.
package com.intellij.build.internal;

import com.intellij.build.BuildContentManager;
import com.intellij.build.BuildViewManager;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FinishBuildEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class DummyBuildViewManager extends BuildViewManager {
  public DummyBuildViewManager(Project project, BuildContentManager buildContentManager) {
    super(project, buildContentManager);
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    if(event instanceof FinishBuildEvent) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println(event.getMessage());
    }
  }
}
