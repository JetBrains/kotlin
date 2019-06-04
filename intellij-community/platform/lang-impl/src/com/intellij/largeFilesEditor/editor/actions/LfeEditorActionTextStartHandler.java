// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor.actions;

import com.intellij.largeFilesEditor.actions.LfeBaseEditorActionHandler;
import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LfeEditorActionTextStartHandler extends LfeBaseEditorActionHandler {
  private static final Logger LOG = Logger.getInstance(LfeEditorActionTextStartHandler.class);

  public LfeEditorActionTextStartHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  protected void doExecuteInLfe(@NotNull EditorManager editorManager,
                                @NotNull Editor editor,
                                @Nullable Caret caret,
                                DataContext dataContext) {
    editorManager.getEditorModel().setCaretToFileStartAndShow();
  }

  @Override
  protected boolean isEnabledInLfe(@NotNull EditorManager editorManager,
                                   @NotNull Editor editor,
                                   @NotNull Caret caret,
                                   DataContext dataContext) {
    return true;
  }
}
