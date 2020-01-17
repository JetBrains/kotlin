// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.intellij.execution.services.ServiceViewActionProvider.getSelectedView;

public class OpenInNewTabActionGroup extends DefaultActionGroup implements DumbAware {
  @Override
  public void update(@NotNull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    e.getPresentation().setEnabled(selectedView != null);
    e.getPresentation().putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, selectedView != null &&
                                                                           selectedView.getSelectedItems().size() == 1);
  }

  @Override
  public boolean canBePerformed(@NotNull DataContext context) {
    return true;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ServiceView selectedView = getSelectedView(e);
    if (selectedView == null) return;

    if (selectedView.getSelectedItems().size() == 1) {
      AnAction[] children = getChildren(e);
      for (AnAction child : children) {
        if (child instanceof OpenInNewTabAction) {
          ActionUtil.performActionDumbAwareWithCallbacks(child, e, e.getDataContext());
          return;
        }
      }
    }

    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, this, e.getDataContext(), true, null, 5);
    popup.setAdText("Drag node onto tool window header to open a new tab", SwingConstants.LEFT);

    if (e.isFromActionToolbar()) {
      Component source = ObjectUtils.tryCast(e.getInputEvent().getSource(), Component.class);
      if (source != null) {
        popup.showUnderneathOf(source);
        return;
      }
    }
    popup.showInBestPositionFor(e.getDataContext());
  }
}
