// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class LfeActionDisabled extends LfeBaseProxyAction {

  private static final Logger logger = Logger.getInstance(LfeActionDisabled.class);

  public LfeActionDisabled(AnAction originalAction) {
    super(originalAction);
  }

  @Override
  protected void updateForLfe(AnActionEvent e, @NotNull EditorManager editorManager) {
    e.getPresentation().setEnabled(false);
  }

  @Override
  protected void actionPerformedForLfe(AnActionEvent e, @NotNull EditorManager editorManager) {
    // never called
    logger.warn("Called code, that shouldn't be called. toString()=" + this.toString());
  }
}
