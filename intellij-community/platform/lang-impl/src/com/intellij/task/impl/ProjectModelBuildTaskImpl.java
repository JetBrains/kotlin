// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.task.ProjectModelBuildTask;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class ProjectModelBuildTaskImpl<T extends ProjectModelBuildableElement> extends AbstractBuildTask
  implements ProjectModelBuildTask<T> {
  private final T myBuildableElement;

  public ProjectModelBuildTaskImpl(T buildableElement, boolean isIncrementalBuild) {
    super(isIncrementalBuild);
    myBuildableElement = buildableElement;
  }

  @Override
  public T getBuildableElement() {
    return myBuildableElement;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Project model element '" + myBuildableElement + "' build task";
  }
}
