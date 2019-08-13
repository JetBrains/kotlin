// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LfeBaseEditorActionHandler extends EditorActionHandler {
  private final EditorActionHandler originalHandler;

  public LfeBaseEditorActionHandler(EditorActionHandler originalHandler) {
    this.originalHandler = originalHandler;
  }

  @Override
  protected final void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    EditorManager editorManager = Utils.tryGetLargeFileEditorManagerFromEditor(editor);
    if (editorManager != null) {
      doExecuteInLfe(editorManager, editor, caret, dataContext);
    }
    else {
      if (originalHandler != null) {
        originalHandler.execute(editor, caret, dataContext);
      }
    }
  }

  @Override
  protected final boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    EditorManager editorManager = Utils.tryGetLargeFileEditorManagerFromEditor(editor);
    if (editorManager != null) {
      return isEnabledInLfe(editorManager, editor, caret, dataContext);
    }
    else {
      return originalHandler != null
             ? originalHandler.isEnabled(editor, caret, dataContext)
             : false;
    }
  }

  protected final EditorActionHandler getOriginalHandler() {
    return originalHandler;
  }

  protected abstract void doExecuteInLfe(@NotNull EditorManager editorManager,
                                         @NotNull Editor editor,
                                         @Nullable Caret caret,
                                         DataContext dataContext);

  protected abstract boolean isEnabledInLfe(@NotNull EditorManager editorManager,
                                            @NotNull Editor editor,
                                            @NotNull Caret caret,
                                            DataContext dataContext);
}
