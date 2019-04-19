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
package com.intellij.openapi.module;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class UnknownModuleType extends ModuleType {
  private final ModuleType myModuleType;

  public UnknownModuleType(String id, ModuleType moduleType) {
    super(id);
    myModuleType = moduleType;
  }

  @NotNull
  @Override
  public ModuleBuilder createModuleBuilder() {
    return myModuleType.createModuleBuilder();
  }

  @NotNull
  @Override
  public String getName() {
    return ProjectBundle.message("module.type.unknown.name", myModuleType.getName());
  }

  @NotNull
  @Override
  public String getDescription() {
    return myModuleType.getDescription();
  }

  @Override
  public Icon getNodeIcon(boolean isOpened) {
    return myModuleType.getIcon();
  }

  @NotNull
  @Override
  public ModuleWizardStep[] createWizardSteps(@NotNull final WizardContext wizardContext, @NotNull final ModuleBuilder moduleBuilder, @NotNull final ModulesProvider modulesProvider) {
    return myModuleType.createWizardSteps(wizardContext, moduleBuilder, modulesProvider);
  }

}
