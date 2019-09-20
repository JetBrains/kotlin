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
package com.intellij.packaging.artifacts;

import com.intellij.packaging.elements.CompositePackagingElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public abstract class ArtifactTemplate {

  public abstract String getPresentableName();

  @Nullable
  public NewArtifactConfiguration createArtifact() {
    final String name = "unnamed";
    return new NewArtifactConfiguration(null, name, null);
  }

  public void setUpArtifact(@NotNull Artifact artifact, @NotNull NewArtifactConfiguration configuration) {
  }

  public static class NewArtifactConfiguration {
    private final CompositePackagingElement<?> myRootElement;
    private final String myArtifactName;
    private final ArtifactType myArtifactType;

    public NewArtifactConfiguration(CompositePackagingElement<?> rootElement, String artifactName, ArtifactType artifactType) {
      myRootElement = rootElement;
      myArtifactName = artifactName;
      myArtifactType = artifactType;
    }

    public CompositePackagingElement<?> getRootElement() {
      return myRootElement;
    }

    public String getArtifactName() {
      return myArtifactName;
    }

    public ArtifactType getArtifactType() {
      return myArtifactType;
    }
  }
}
