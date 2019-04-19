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

import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.packaging.elements.CompositePackagingElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public interface ModifiableArtifactModel extends ArtifactModel {

  @NotNull
  ModifiableArtifact addArtifact(final @NotNull String name, @NotNull ArtifactType artifactType);

  @NotNull
  ModifiableArtifact addArtifact(final @NotNull String name, @NotNull ArtifactType artifactType, CompositePackagingElement<?> rootElement);

  @NotNull
  ModifiableArtifact addArtifact(@NotNull String name, @NotNull ArtifactType artifactType, CompositePackagingElement<?> rootElement,
                                 @Nullable ProjectModelExternalSource externalSource);

  void removeArtifact(@NotNull Artifact artifact);

  @NotNull
  ModifiableArtifact getOrCreateModifiableArtifact(@NotNull Artifact artifact);

  @Nullable
  Artifact getModifiableCopy(Artifact artifact);

  void addListener(@NotNull ArtifactListener listener);

  void removeListener(@NotNull ArtifactListener listener);


  boolean isModified();

  void commit();

  void dispose();
}
