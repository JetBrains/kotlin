// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class ChangeHierarchyViewActionBase extends ToggleAction {
  public ChangeHierarchyViewActionBase(String text, String description, Icon icon) {super(text, description, icon);}

  @Override
  public final boolean isSelected(@NotNull final AnActionEvent event) {
    final HierarchyBrowserBaseEx browser = getHierarchyBrowser(event.getDataContext());
    return browser != null && getTypeName().equals(browser.getCurrentViewType());
  }

  protected abstract String getTypeName();

  @Override
  public final void setSelected(@NotNull final AnActionEvent event, final boolean flag) {
    if (flag) {
      final HierarchyBrowserBaseEx browser = getHierarchyBrowser(event.getDataContext());
      ApplicationManager.getApplication().invokeLater(() -> {
        if (browser != null) {
          browser.changeView(getTypeName());
        }
      });
    }
  }

  @Override
  public void update(@NotNull final AnActionEvent event) {
    super.update(event);
    final Presentation presentation = event.getPresentation();
    final HierarchyBrowserBaseEx browser = getHierarchyBrowser(event.getDataContext());
    presentation.setEnabled(browser != null && browser.isValidBase());
  }

  protected abstract HierarchyBrowserBaseEx getHierarchyBrowser(DataContext context);
}
