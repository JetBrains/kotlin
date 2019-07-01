// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.layout.impl.RunnerContentUi;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RestoreViewAction extends DumbAwareToggleAction {

  private final RunnerContentUi myUi;
  private final Content myContent;

  public RestoreViewAction(@NotNull RunnerContentUi ui, @NotNull Content content) {
    myUi = ui;
    myContent = content;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return myContent.isValid() && Objects.requireNonNull(myContent.getManager()).getIndexOfContent(myContent) != -1;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    if (state) {
      myUi.restore(myContent);
    } else {
      myUi.minimize(myContent, null);
    }
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    super.update(e);
    e.getPresentation().setText(myContent.getDisplayName());
    if (isSelected(e)) {
      e.getPresentation().setEnabled(myUi.getContentManager().getContents().length > 1);
    }
  }

  public Content getContent() {
    return myContent;
  }
}
