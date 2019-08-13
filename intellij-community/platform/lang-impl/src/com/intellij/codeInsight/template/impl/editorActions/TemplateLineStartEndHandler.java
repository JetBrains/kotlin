// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.impl.editorActions;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TemplateLineStartEndHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;
  private final boolean myIsHomeHandler;
  private final boolean myWithSelection;

  public TemplateLineStartEndHandler(final EditorActionHandler originalHandler, boolean isHomeHandler, boolean withSelection) {
    super(true);
    myOriginalHandler = originalHandler;
    myIsHomeHandler = isHomeHandler;
    myWithSelection = withSelection;
  }

  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null && !templateState.isFinished()) {
      TextRange range = templateState.getCurrentVariableRange();
      int caretOffset = editor.getCaretModel().getOffset();
      if (range != null && range.containsOffset(caretOffset)) return true;
    }
    return myOriginalHandler.isEnabled(editor, caret, dataContext);
  }

  @Override
  protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    final TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null && !templateState.isFinished()) {
      final TextRange range = templateState.getCurrentVariableRange();
      final int caretOffset = editor.getCaretModel().getOffset();
      if (range != null && shouldStayInsideVariable(range, caretOffset)) {
        int selectionOffset = editor.getSelectionModel().getLeadSelectionOffset();
        int offsetToMove = myIsHomeHandler ? range.getStartOffset() : range.getEndOffset();
        editor.getCaretModel().moveToOffset(offsetToMove);
        EditorModificationUtil.scrollToCaret(editor);
        if (myWithSelection) {
          editor.getSelectionModel().setSelection(selectionOffset, offsetToMove);
        }
        else {
          editor.getSelectionModel().removeSelection();
        }
        return;
      }
    }
    myOriginalHandler.execute(editor, caret, dataContext);
  }

  private boolean shouldStayInsideVariable(TextRange varRange, int caretOffset) {
    return varRange.containsOffset(caretOffset) &&
           caretOffset != (myIsHomeHandler ? varRange.getStartOffset() : varRange.getEndOffset());
  }
}
