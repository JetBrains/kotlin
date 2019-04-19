// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.structureView.newStructureView;

import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.TreeAction;
import com.intellij.openapi.actionSystem.ActionWithDelegate;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class TreeActionWrapper extends ToggleAction implements DumbAware, ActionWithDelegate<TreeAction> {
  private final TreeAction myAction;
  private final TreeActionsOwner myStructureView;

  public TreeActionWrapper(@NotNull TreeAction action, @NotNull TreeActionsOwner structureView) {
    myAction = action;
    myStructureView = structureView;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    Presentation presentation = e.getPresentation();
    ActionPresentation actionPresentation = myAction.getPresentation();
    if (presentation.getClientProperty(MenuItemPresentationFactory.HIDE_ICON) == null) {
      presentation.setIcon(actionPresentation.getIcon());
    }
    presentation.setText(actionPresentation.getText());
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return TreeModelWrapper.isActive(myAction, myStructureView);
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    myStructureView.setActionActive(myAction.getName(), TreeModelWrapper.shouldRevert(myAction) != state);
  }

  @NotNull
  @Override
  public TreeAction getDelegate() {
    return myAction;
  }

  @Override
  public String getPresentableName() {
    return myAction.getName();
  }
}
