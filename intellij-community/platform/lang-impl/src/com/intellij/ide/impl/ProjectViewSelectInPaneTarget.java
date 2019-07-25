// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl;

import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.project.PossiblyDumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ProjectViewSelectInPaneTarget extends ProjectViewSelectInTarget implements PossiblyDumbAware {
  private final AbstractProjectViewPane myPane;
  private final boolean myDumbAware;

  public ProjectViewSelectInPaneTarget(@NotNull Project project, @NotNull AbstractProjectViewPane pane, boolean dumbAware) {
    super(project);
    myPane = pane;
    myDumbAware = dumbAware;
  }

  @Override
  public String toString() {
    return myPane.getTitle();
  }

  @Override
  public String getMinorViewId() {
    return myPane.getId();
  }

  @Override
  public float getWeight() {
    return myPane.getWeight();
  }

  @Override
  public boolean isDumbAware() {
    return myDumbAware;
  }
}
