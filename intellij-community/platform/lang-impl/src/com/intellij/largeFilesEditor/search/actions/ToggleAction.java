// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ComponentNotRegistered")
public class ToggleAction extends CheckboxAction implements DumbAware {
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getInstance(ToggleAction.class);
  private final SearchManager searchManager;

  boolean isSelected = false;

  public ToggleAction(SearchManager searchManager, String name) {
    super(name);
    this.searchManager = searchManager;
  }

  @Override
  public boolean isSelected(@Nullable AnActionEvent e) {
    return isSelected;
  }

  @Override
  public void setSelected(@Nullable AnActionEvent e, boolean state) {
    if (isSelected != state) {
      isSelected = state;
      searchManager.onSearchParametersChanged();
    }
  }
}