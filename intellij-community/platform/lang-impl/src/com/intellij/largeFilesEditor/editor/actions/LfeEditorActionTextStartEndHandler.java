// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor.actions;

import com.intellij.largeFilesEditor.actions.LfeBaseEditorActionHandler;
import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LfeEditorActionTextStartEndHandler extends LfeBaseEditorActionHandler {

  public LfeEditorActionTextStartEndHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  protected void doExecuteInLfe(@NotNull EditorManager editorManager,
                                @NotNull Editor editor,
                                @Nullable Caret caret,
                                DataContext dataContext) {
    if (isStart()) {
      editorManager.getEditorModel().setCaretToFileStartAndShow();
    }
    else {
      editorManager.getEditorModel().setCaretToFileEndAndShow();
    }

    IdeDocumentHistory docHistory = IdeDocumentHistory.getInstance(editorManager.getProject());
    if (docHistory != null) {
      docHistory.includeCurrentCommandAsNavigation();
      docHistory.includeCurrentCommandHasMoves();
    }
  }

  @Override
  protected boolean isEnabledInLfe(@NotNull EditorManager editorManager,
                                   @NotNull Editor editor,
                                   @NotNull Caret caret,
                                   DataContext dataContext) {
    return true;
  }

  abstract protected boolean isStart();
}
