/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.task.impl;

import com.intellij.task.ProjectTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class ProjectTaskList extends ArrayList<ProjectTask> implements ProjectTask {
  public ProjectTaskList(@NotNull Collection<? extends ProjectTask> c) {
    super(c);
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return toString();
  }
}
