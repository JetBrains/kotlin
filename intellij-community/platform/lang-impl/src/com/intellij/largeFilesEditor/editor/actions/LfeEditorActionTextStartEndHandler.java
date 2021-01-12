// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor.actions;

import com.intellij.largeFilesEditor.actions.LfeBaseEditorActionHandler;
import com.intellij.largeFilesEditor.editor.LargeFileEditor;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LfeEditorActionTextStartEndHandler extends LfeBaseEditorActionHandler {

  private final boolean isStart;

  public LfeEditorActionTextStartEndHandler(EditorActionHandler originalHandler, boolean start) {
    super(originalHandler);
    isStart = start;
  }

  @Override
  protected void doExecuteInLfe(@NotNull LargeFileEditor largeFileEditor,
                                @NotNull Editor editor,
                                @Nullable Caret caret,
                                DataContext dataContext) {
    if (isStart) {
      largeFileEditor.getEditorModel().setCaretToFileStartAndShow();
    }
    else {
      largeFileEditor.getEditorModel().setCaretToFileEndAndShow();
    }

    IdeDocumentHistory docHistory = IdeDocumentHistory.getInstance(largeFileEditor.getProject());
    if (docHistory != null) {
      docHistory.includeCurrentCommandAsNavigation();
      docHistory.setCurrentCommandHasMoves();
    }
  }

  @Override
  protected boolean isEnabledInLfe(@NotNull LargeFileEditor largeFileEditor,
                                   @NotNull Editor editor,
                                   @NotNull Caret caret,
                                   DataContext dataContext) {
    return true;
  }
}
