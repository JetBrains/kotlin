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

import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.elements.CompositePackagingElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Describes an artifact configuration. Use {@link ArtifactManager} to create new and get existing artifacts.
 *
 * @author nik
 */
public interface Artifact extends UserDataHolder, ProjectModelBuildableElement {
  @NotNull
  ArtifactType getArtifactType();

  String getName();

  boolean isBuildOnMake();

  /**
   * @return the root element in the artifact's output layout tree
   */
  @NotNull
  CompositePackagingElement<?> getRootElement();

  @Nullable
  String getOutputPath();

  Collection<? extends ArtifactPropertiesProvider> getPropertiesProviders();

  ArtifactProperties<?> getProperties(@NotNull ArtifactPropertiesProvider propertiesProvider);

  @Nullable
  VirtualFile getOutputFile();

  @Nullable
  String getOutputFilePath();
}
