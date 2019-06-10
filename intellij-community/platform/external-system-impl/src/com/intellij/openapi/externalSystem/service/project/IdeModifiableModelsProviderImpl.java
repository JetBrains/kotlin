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
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import org.jetbrains.annotations.NotNull;

public class IdeModifiableModelsProviderImpl extends AbstractIdeModifiableModelsProvider {

  private final LibraryTable.ModifiableModel myLibrariesModel;

  public IdeModifiableModelsProviderImpl(Project project) {
    super(project);
    myLibrariesModel = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject).getModifiableModel();
  }

  @NotNull
  @Override
  public LibraryTable.ModifiableModel getModifiableProjectLibrariesModel() {
    return myLibrariesModel;
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
}
