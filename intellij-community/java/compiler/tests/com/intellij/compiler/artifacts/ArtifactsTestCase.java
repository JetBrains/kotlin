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
package com.intellij.compiler.artifacts;

import com.intellij.facet.Facet;
import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.roots.ui.configuration.artifacts.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.ManifestFileProvider;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.packaging.ui.ArtifactEditor;
import com.intellij.packaging.ui.ManifestFileConfiguration;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * @author nik
 */
public abstract class ArtifactsTestCase extends IdeaTestCase {
  protected boolean mySetupModule;

  protected ArtifactManager getArtifactManager() {
    return ArtifactManager.getInstance(myProject);
  }

  @Override
  protected void setUpModule() {
    if (mySetupModule) {
      super.setUpModule();
    }
  }

  protected void deleteArtifact(final Artifact artifact) {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.removeArtifact(artifact);
    commitModel(model);
  }

  protected static void commitModel(final ModifiableArtifactModel model) {
    WriteAction.runAndWait(() -> model.commit());
  }

  protected Artifact rename(Artifact artifact, String newName) {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.getOrCreateModifiableArtifact(artifact).setName(newName);
    commitModel(model);
    return artifact;
  }

  protected Artifact addArtifact(String name) {
    return addArtifact(name, null);
  }

  protected Artifact addArtifact(String name, final CompositePackagingElement<?> root) {
    return addArtifact(name, PlainArtifactType.getInstance(), root);
  }

  protected Artifact addArtifact(final String name, final ArtifactType type, final CompositePackagingElement<?> root) {
    return getArtifactManager().addArtifact(name, type, root);
  }

  protected PackagingElementResolvingContext getContext() {
    return ArtifactManager.getInstance(myProject).getResolvingContext();
  }

  public static void renameFile(final VirtualFile file, final String newName) {
    try {
      WriteAction.runAndWait(() -> file.rename(IdeaTestCase.class, newName));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected Module addModule(final String moduleName, final @Nullable VirtualFile sourceRoot) {
    return WriteAction.computeAndWait(() -> {
      final Module module = createModule(moduleName);
      if (sourceRoot != null) {
        PsiTestUtil.addSourceContentToRoots(module, sourceRoot);
      }
      ModuleRootModificationUtil.setModuleSdk(module, getTestProjectJdk());
      return module;
    });
  }

  public static class MockPackagingEditorContext extends ArtifactEditorContextImpl {
    public MockPackagingEditorContext(ArtifactsStructureConfigurableContext parent, final ArtifactEditorEx editor) {
      super(parent, editor);
    }

    @Override
    public void selectArtifact(@NotNull Artifact artifact) {
    }

    @Override
    public void selectFacet(@NotNull Facet<?> facet) {
    }

    @Override
    public void selectModule(@NotNull Module module) {
    }

    @Override
    public void selectLibrary(@NotNull Library library) {
    }

    @Override
    public void queueValidation() {
    }

    @Override
    public List<Artifact> chooseArtifacts(List<? extends Artifact> artifacts, String title) {
      return new ArrayList<>(artifacts);
    }

    @Override
    public List<Module> chooseModules(List<Module> modules, String title) {
      return modules;
    }

    @Override
    public List<Library> chooseLibraries(String title) {
      return Collections.emptyList();
    }
  }

  public class MockArtifactsStructureConfigurableContext implements ArtifactsStructureConfigurableContext {
    private ModifiableArtifactModel myModifiableModel;
    private final Map<Module, ModifiableRootModel> myModifiableRootModels = new HashMap<>();
    private final Map<CompositePackagingElement<?>, ManifestFileConfiguration> myManifestFiles =
      new HashMap<>();
    private final ArtifactEditorManifestFileProvider myManifestFileProvider = new ArtifactEditorManifestFileProvider(this);

    @Override
    @NotNull
    public ModifiableArtifactModel getOrCreateModifiableArtifactModel() {
      if (myModifiableModel == null) {
        myModifiableModel = ArtifactManager.getInstance(myProject).createModifiableModel();
      }
      return myModifiableModel;
    }

    @Override
    public ModifiableModuleModel getModifiableModuleModel() {
      return null;
    }

    @Override
    @NotNull
    public ModifiableRootModel getOrCreateModifiableRootModel(@NotNull Module module) {
      ModifiableRootModel model = myModifiableRootModels.get(module);
      if (model == null) {
        model = ModuleRootManager.getInstance(module).getModifiableModel();
        myModifiableRootModels.put(module, model);
      }
      return model;
    }

    @Override
    public ArtifactEditorSettings getDefaultSettings() {
      return new ArtifactEditorSettings();
    }

    @Override
    @NotNull
    public Project getProject() {
      return myProject;
    }

    @Override
    @NotNull
    public ArtifactModel getArtifactModel() {
      if (myModifiableModel != null) {
        return myModifiableModel;
      }
      return ArtifactManager.getInstance(myProject);
    }

    public void commitModel() {
      if (myModifiableModel != null) {
        myModifiableModel.commit();
      }
    }

    @Override
    @NotNull
    public ModulesProvider getModulesProvider() {
      return new DefaultModulesProvider(myProject);
    }

    @Override
    @NotNull
    public FacetsProvider getFacetsProvider() {
      return DefaultFacetsProvider.INSTANCE;
    }

    @Override
    public Library findLibrary(@NotNull String level, @NotNull String libraryName) {
      return ArtifactManager.getInstance(myProject).getResolvingContext().findLibrary(level, libraryName);
    }

    @NotNull
    @Override
    public ManifestFileProvider getManifestFileProvider() {
      return myManifestFileProvider;
    }

    @Override
    public ManifestFileConfiguration getManifestFile(CompositePackagingElement<?> element, ArtifactType artifactType) {
      final VirtualFile manifestFile = ManifestFileUtil.findManifestFile(element, this, PlainArtifactType.getInstance());
      if (manifestFile == null) {
        return null;
      }

      ManifestFileConfiguration configuration = myManifestFiles.get(element);
      if (configuration == null) {
        configuration = ManifestFileUtil.createManifestFileConfiguration(manifestFile);
        myManifestFiles.put(element, configuration);
      }
      return configuration;
    }

    @Override
    public CompositePackagingElement<?> getRootElement(@NotNull Artifact artifact) {
      return artifact.getRootElement();
    }

    @Override
    public void editLayout(@NotNull Artifact artifact, Runnable action) {
      final ModifiableArtifact modifiableArtifact = getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(artifact);
      modifiableArtifact.setRootElement(artifact.getRootElement());
      action.run();
    }

    @Override
    public ArtifactEditor getOrCreateEditor(Artifact artifact) {
      throw new UnsupportedOperationException("'getOrCreateEditor' not implemented in " + getClass().getName());
    }

    @Override
    @NotNull
    public Artifact getOriginalArtifact(@NotNull Artifact artifact) {
      if (myModifiableModel != null) {
        return myModifiableModel.getOriginalArtifact(artifact);
      }
      return artifact;
    }

    @Override
    public void queueValidation(Artifact artifact) {
    }

    @Override
    @NotNull
    public ArtifactProjectStructureElement getOrCreateArtifactElement(@NotNull Artifact artifact) {
      throw new UnsupportedOperationException("'getOrCreateArtifactElement' not implemented in " + getClass().getName());
    }
  }
}
