// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StatusTextAction extends com.intellij.find.editorHeaderActions.StatusTextAction {
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getInstance(StatusTextAction.class);
  private final SearchManager searchManager;

  public StatusTextAction(SearchManager searchManager) {
    super();
    this.searchManager = searchManager;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    searchManager.updateStatusText();
    String statusText = searchManager.getStatusText();

    JLabel label = (JLabel)e.getPresentation().getClientProperty(COMPONENT_KEY);
    if (label != null) {
      label.setText(statusText);
      label.setVisible(StringUtil.isNotEmpty(statusText));
    }
  }
}
