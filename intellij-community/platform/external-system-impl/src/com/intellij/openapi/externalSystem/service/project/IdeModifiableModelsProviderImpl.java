/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.project;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.CompositePackagingElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class IdeModifiableModelsProviderImpl extends AbstractIdeModifiableModelsProvider {

  private final LibraryTable.ModifiableModel myLibrariesModel;

  public IdeModifiableModelsProviderImpl(Project project) {
    super(project);
    myLibrariesModel = ProjectLibraryTable.getInstance(myProject).getModifiableModel();
  }

  @NotNull
  @Override
  public LibraryTable.ModifiableModel getModifiableProjectLibrariesModel() {
    return myLibrariesModel;
  }

  @Override
  protected ModifiableArtifactModel doGetModifiableArtifactModel() {
    return ReadAction.compute(() -> {
      // todo move this to external system java module
      ArtifactManager artifactManager = ArtifactManager.getInstance(myProject);
      return artifactManager != null ? artifactManager.createModifiableModel() : new DummyArtifactModel();
    });
  }

  @Override
  protected ModifiableModuleModel doGetModifiableModuleModel() {
    return ReadAction.compute(() -> ModuleManager.getInstance(myProject).getModifiableModel());
  }

  @Override
  @NotNull
  protected ModifiableRootModel doGetModifiableRootModel(@NotNull final Module module) {
    return ReadAction.compute(() -> ModuleRootManager.getInstance(module).getModifiableModel());
  }

  @Override
  protected ModifiableFacetModel doGetModifiableFacetModel(Module module) {
    return FacetManager.getInstance(module).createModifiableModel();
  }

  @Override
  protected Library.ModifiableModel doGetModifiableLibraryModel(Library library) {
    return library.getModifiableModel();
  }

  private static class DummyArtifactModel implements ModifiableArtifactModel {
    @NotNull
    @Override
    public ModifiableArtifact addArtifact(@NotNull String name, @NotNull ArtifactType artifactType) {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ModifiableArtifact addArtifact(@NotNull String name,
                                          @NotNull ArtifactType artifactType,
                                          CompositePackagingElement<?> rootElement) {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public ModifiableArtifact addArtifact(@NotNull String name,
                                          @NotNull ArtifactType artifactType,
                                          CompositePackagingElement<?> rootElement,
                                          @Nullable ProjectModelExternalSource externalSource) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removeArtifact(@NotNull Artifact artifact) {
    }

    @NotNull
    @Override
    public ModifiableArtifact getOrCreateModifiableArtifact(@NotNull Artifact artifact) {
      throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public Artifact getModifiableCopy(Artifact artifact) {
      return null;
    }

    @Override
    public void addListener(@NotNull ArtifactListener listener) {
    }

    @Override
    public void removeListener(@NotNull ArtifactListener listener) {
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public void commit() {
    }

    @Override
    public void dispose() {
    }

    @NotNull
    @Override
    public Artifact[] getArtifacts() {
      return new Artifact[0];
    }

    @Nullable
    @Override
    public Artifact findArtifact(@NotNull String name) {
      return null;
    }

    @NotNull
    @Override
    public Artifact getArtifactByOriginal(@NotNull Artifact artifact) {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Artifact getOriginalArtifact(@NotNull Artifact artifact) {
      throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Collection<? extends Artifact> getArtifactsByType(@NotNull ArtifactType type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends Artifact> getAllArtifactsIncludingInvalid() {
      throw new UnsupportedOperationException();
    }
  }
}
