// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.internal;

import com.intellij.build.BuildViewManager;
import com.intellij.build.events.BuildEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

/**
 * @author Vladislav.Soroka
 */
@TestOnly
public class DummyBuildViewManager extends BuildViewManager {
  public DummyBuildViewManager(Project project) {
    super(project);
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {}
}
