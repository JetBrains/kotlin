// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BraceOrQuoteOutAction extends EditorAction {
  public BraceOrQuoteOutAction() {
    super(new Handler());
  }

  private static class Handler extends EditorActionHandler {
    private Handler() {
      super(true);
    }

    @Override
    protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
      int caretOffset = caret.getOffset();
      return TabOutScopesTracker.getInstance().hasScopeEndingAt(editor, caretOffset);
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      assert caret != null;
      int caretOffset = caret.getOffset();
      if (TabOutScopesTracker.getInstance().removeScopeEndingAt(editor, caretOffset)) {
        caret.moveToOffset(caretOffset + 1);
      }
    }
  }
}
