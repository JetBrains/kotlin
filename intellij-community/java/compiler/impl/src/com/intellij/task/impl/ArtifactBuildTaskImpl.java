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

import com.intellij.packaging.artifacts.Artifact;
import com.intellij.task.ArtifactBuildTask;
import com.intellij.task.ProjectModelBuildTask;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated to be removed, use {@link ProjectModelBuildTask}
 *
 * @author Vladislav.Soroka
 */
@Deprecated
public class ArtifactBuildTaskImpl extends AbstractBuildTask implements ArtifactBuildTask {
  private final Artifact myArtifact;

  public ArtifactBuildTaskImpl(Artifact artifact, boolean isIncrementalBuild) {
    super(isIncrementalBuild);
    myArtifact = artifact;
  }

  @Override
  public Artifact getArtifact() {
    return myArtifact;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Artifact '" + myArtifact.getName() + "' build task";
  }
}
