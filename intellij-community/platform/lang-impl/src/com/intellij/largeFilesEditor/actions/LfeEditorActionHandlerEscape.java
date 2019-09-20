// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.find.SearchReplaceComponent;
import com.intellij.largeFilesEditor.editor.LargeFileEditor;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LfeEditorActionHandlerEscape extends LfeBaseEditorActionHandler {

  public LfeEditorActionHandlerEscape(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  protected void doExecuteInLfe(@NotNull LargeFileEditor largeFileEditor,
                                @NotNull Editor editor,
                                @Nullable Caret caret,
                                DataContext dataContext) {
    if (largeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent) {
      largeFileEditor.getSearchManager().onEscapePressed();
    }
    else if (getOriginalHandler().isEnabled(editor, caret, dataContext)) {
      getOriginalHandler().execute(editor, caret, dataContext);
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
