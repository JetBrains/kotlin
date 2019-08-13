// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

public class NewElementSamePlaceAction extends NewElementAction {
  @Override
  protected String getPopupTitle() {
    return IdeBundle.message("title.popup.new.element.same.place");
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    return e.getData(LangDataKeys.IDE_VIEW) != null;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ListPopup popup = createPopup(e.getDataContext());
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null) {
      popup.showCenteredInCurrentWindow(project);
    }
    else {
      popup.showInBestPositionFor(e.getDataContext());
    }
  }
}