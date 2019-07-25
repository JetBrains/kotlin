/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.ide.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.impl.ShowModulesAction;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.module.ModuleGrouperKt;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author nik
 */
public class FlattenModulesToggleAction extends ToggleAction implements DumbAware {
  private final BooleanSupplier myIsEnabled;
  private final BooleanSupplier myIsSelected;
  private final Consumer<? super Boolean> mySetSelected;
  private final Project myProject;

  public FlattenModulesToggleAction(Project project, BooleanSupplier isEnabled, BooleanSupplier isSelected, Consumer<? super Boolean> setSelected) {
    super(ProjectBundle.message("project.roots.flatten.modules.action.text"), ProjectBundle.message("project.roots.flatten.modules.action.description"), AllIcons.ObjectBrowser.FlattenModules);
    myIsEnabled = isEnabled;
    myIsSelected = isSelected;
    mySetSelected = setSelected;
    this.myProject = project;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(ModuleGrouperKt.isQualifiedModuleNamesEnabled(myProject) && ShowModulesAction.hasModules());
    if (!myIsEnabled.getAsBoolean()) {
      presentation.setEnabled(false);
      if (ActionPlaces.isPopupPlace(e.getPlace()) || ToolWindowContentUi.POPUP_PLACE.equals(e.getPlace())) {
        presentation.setVisible(false);
      }
    }
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return myIsSelected.getAsBoolean();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    mySetSelected.accept(state);
  }
}
