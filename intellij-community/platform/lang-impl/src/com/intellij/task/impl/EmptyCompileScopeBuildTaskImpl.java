// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.task.EmptyCompileScopeBuildTask;
import org.jetbrains.annotations.NotNull;

/**
 * This is task is the opposite to {@link com.intellij.task.ProjectModelBuildTask}.
 * The task can be used to invoke 'empty' compilation that will by default trigger all configured before- and after- compilation tasks.
 * The interpretation of the 'isIncremental' flag is up to the runner that will actually execute this task.
 */
public class EmptyCompileScopeBuildTaskImpl extends AbstractBuildTask implements EmptyCompileScopeBuildTask {

  public EmptyCompileScopeBuildTaskImpl(boolean isIncrementalBuild) {
    super(isIncrementalBuild);
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Empty compilation scope build task";
  }
}
