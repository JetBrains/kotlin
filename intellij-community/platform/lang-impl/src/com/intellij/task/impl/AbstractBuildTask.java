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

import com.intellij.task.BuildTask;
import com.intellij.task.ProjectTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractBuildTask extends AbstractProjectTask implements BuildTask {
  private final boolean myIsIncrementalBuild;

  public AbstractBuildTask(boolean isIncrementalBuild) {
    this(isIncrementalBuild, Collections.emptyList());
  }

  public AbstractBuildTask(boolean isIncrementalBuild, @NotNull List<ProjectTask> dependencies) {
    super(dependencies);
    myIsIncrementalBuild = isIncrementalBuild;
  }

  @Override
  public boolean isIncrementalBuild() {
    return myIsIncrementalBuild;
  }
}
