// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Experimental
public interface ProjectTaskManagerListener {
  void beforeRun(@NotNull ProjectTaskContext context) throws ExecutionException;

  void afterRun(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult result) throws ExecutionException;
}
