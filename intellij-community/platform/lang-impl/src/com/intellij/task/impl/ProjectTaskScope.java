// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.openapi.util.Key;
import com.intellij.task.ProjectTask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ApiStatus.Experimental
public interface ProjectTaskScope {
  Key<ProjectTaskScope> KEY = Key.create("project task scope");
  @NotNull
  <T extends ProjectTask> List<T> getRequestedTasks(@NotNull Class<T> instanceOf);
}
