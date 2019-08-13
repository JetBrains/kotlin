// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextParameterAfterCompletionHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public NextParameterAfterCompletionHandler(EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    return myOriginalHandler.isEnabled(editor, caret, dataContext);
  }

  @Override
  protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    Ref<Boolean> documentChanged = new Ref<>();
    DocumentListener listener = new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        if (event.getOldLength() > 0 || event.getNewLength() > 0) documentChanged.set(Boolean.TRUE);
      }
    };
    editor.getDocument().addDocumentListener(listener);
    try {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
    finally {
      editor.getDocument().removeDocumentListener(listener);
    }
    if (documentChanged.isNull()) {
      ActionManager actionManager = ActionManager.getInstance();
      KeyboardShortcut completionShortcut = actionManager.getKeyboardShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_REPLACE);
      KeyboardShortcut parameterShortcut = actionManager.getKeyboardShortcut(IdeActions.ACTION_EDITOR_NEXT_PARAMETER);
      if (completionShortcut != null && completionShortcut.equals(parameterShortcut)) {
        EditorActionHandler parameterHandler = EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_NEXT_PARAMETER);
        if (parameterHandler.isEnabled(editor, caret, dataContext)) parameterHandler.execute(editor, caret, dataContext);
      }
    }
  }
}
