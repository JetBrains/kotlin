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
package com.intellij.packaging.elements;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.artifacts.ArtifactModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public interface PackagingElementResolvingContext {
  @NotNull
  Project getProject();

  @NotNull
  ArtifactModel getArtifactModel();

  @NotNull
  ModulesProvider getModulesProvider();

  @NotNull
  FacetsProvider getFacetsProvider();

  @Nullable
  Library findLibrary(@NotNull String level, @NotNull String libraryName);

  @NotNull
  ManifestFileProvider getManifestFileProvider();
}
