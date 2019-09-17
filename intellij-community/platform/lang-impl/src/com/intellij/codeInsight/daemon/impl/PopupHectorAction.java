// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class PopupHectorAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);

    HectorComponent hector = ServiceManager.getService(file.getProject(), HectorComponentFactory.class).create(file);
    hector.showComponent(JBPopupFactory.getInstance().guessBestPopupLocation(dataContext));
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PSI_FILE) != null);
  }
}