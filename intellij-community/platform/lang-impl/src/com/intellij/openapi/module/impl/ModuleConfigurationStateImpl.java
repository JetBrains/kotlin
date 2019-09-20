/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.module.impl;

import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.UserDataHolderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModuleConfigurationStateImpl extends UserDataHolderBase implements ModuleConfigurationState {
  private final ModulesProvider myProvider;
  private final Project myProject;

  public ModuleConfigurationStateImpl(@NotNull Project project, @NotNull ModulesProvider provider) {
    myProvider = provider;
    myProject = project;
  }

  @Override
  public ModulesProvider getModulesProvider() {
    return myProvider;
  }

  @Override
  public FacetsProvider getFacetsProvider() {
    return DefaultFacetsProvider.INSTANCE;
  }

  @Override
  @Nullable
  public ModifiableRootModel getRootModel() {
    return null;
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }
}
