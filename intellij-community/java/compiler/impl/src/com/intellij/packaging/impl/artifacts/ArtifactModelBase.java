/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.util.Condition;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactModel;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author nik
 */
public abstract class ArtifactModelBase implements ArtifactModel {
  private Map<String, Artifact> myArtifactsMap;
  private Artifact[] myArtifactsArray;
  public static final Condition<Artifact> VALID_ARTIFACT_CONDITION = artifact -> !(artifact instanceof InvalidArtifact);

  protected abstract List<? extends Artifact> getArtifactsList();

  @Override
  @NotNull
  public Artifact[] getArtifacts() {
    if (myArtifactsArray == null) {
      final List<? extends Artifact> validArtifacts = ContainerUtil.findAll(getArtifactsList(), VALID_ARTIFACT_CONDITION);
      myArtifactsArray = validArtifacts.toArray(new Artifact[0]);
    }
    return myArtifactsArray;
  }

  @Override
  public List<? extends Artifact> getAllArtifactsIncludingInvalid() {
    return Collections.unmodifiableList(getArtifactsList());
  }

  @Override
  public Artifact findArtifact(@NotNull String name) {
    if (myArtifactsMap == null) {
      myArtifactsMap = new HashMap<>();
      for (Artifact artifact : getArtifactsList()) {
        myArtifactsMap.put(artifact.getName(), artifact);
      }
    }
    return myArtifactsMap.get(name);
  }

  @Override
  @NotNull
  public Artifact getArtifactByOriginal(@NotNull Artifact artifact) {
    return artifact;
  }

  @Override
  @NotNull
  public Artifact getOriginalArtifact(@NotNull Artifact artifact) {
    return artifact;
  }

  @Override
  @NotNull
  public Collection<? extends Artifact> getArtifactsByType(@NotNull ArtifactType type) {
    final List<Artifact> result = new ArrayList<>();
    for (Artifact artifact : getArtifacts()) {
      if (artifact.getArtifactType().equals(type)) {
        result.add(artifact);
      }
    }
    return result;
  }

  protected void artifactsChanged() {
    myArtifactsMap = null;
    myArtifactsArray = null;
  }
}
