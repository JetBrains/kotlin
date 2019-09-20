// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.module.ModuleGrouperKt;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.intellij.util.PlatformUtils.isIntelliJ;

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
    presentation.setEnabledAndVisible(isIntelliJ() && ModuleGrouperKt.isQualifiedModuleNamesEnabled(myProject));
    if (!myIsEnabled.getAsBoolean()) {
      presentation.setEnabled(false);
      if (ActionPlaces.isPopupPlace(e.getPlace())) {
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
