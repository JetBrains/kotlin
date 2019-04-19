// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public abstract class ForcedCopyModeAction extends DumbAwareAction {
  private final boolean myRichCopyEnabled;
  
  protected ForcedCopyModeAction(boolean richCopyEnabled) {
    myRichCopyEnabled = richCopyEnabled;
    setEnabledInModalContext(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation p = e.getPresentation();
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    p.setVisible(RichCopySettings.getInstance().isEnabled() != myRichCopyEnabled &&
                 (e.isFromActionToolbar() || (editor != null && editor.getSelectionModel().hasSelection(true))));
    p.setEnabled(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    RichCopySettings settings = RichCopySettings.getInstance();
    boolean savedValue = settings.isEnabled();
    try {
      settings.setEnabled(myRichCopyEnabled);
      ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_COPY).actionPerformed(e);
    }
    finally {
      settings.setEnabled(savedValue);
    }
  }
}
