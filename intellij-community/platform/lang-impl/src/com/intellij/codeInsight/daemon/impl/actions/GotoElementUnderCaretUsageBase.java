// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/*package*/ abstract class GotoElementUnderCaretUsageBase extends BaseCodeInsightAction implements CodeInsightActionHandler {

  @NotNull
  private final Direction myDirection;

  /**
   * @param direction defines direction for next usage searching.
   */
  GotoElementUnderCaretUsageBase(@NotNull Direction direction) {
    myDirection = direction;
  }

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return this;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    final Comparator<Integer> ordering = myDirection.ordering;
    final int caretOffset = editor.getCaretModel().getOffset();
    final int startOffset = file.getTextRange().getStartOffset();
    final int endOffset = file.getTextRange().getEndOffset();
    final Ref<Integer> first = new Ref<>();
    final Ref<Integer> next = new Ref<>();
    DaemonCodeAnalyzerEx.processHighlights(editor.getDocument(), project, null, startOffset, endOffset, info -> {
      if (HighlightInfoType.ELEMENT_UNDER_CARET_READ.equals(info.type) || HighlightInfoType.ELEMENT_UNDER_CARET_WRITE.equals(info.type)) {
        if (ordering.compare(info.startOffset, caretOffset) > 0 && ordering.compare(info.endOffset, caretOffset) > 0) {
          if (next.isNull() || ordering.compare(next.get(), info.startOffset) > 0) {
            next.set(info.startOffset);
          }
        }
        if (first.isNull() || ordering.compare(first.get(), info.startOffset) > 0) {
          first.set(info.startOffset);
        }
      }
      return true;
    });
    if (!next.isNull()) {
      moveCaret(editor, next.get());
    } else if (!first.isNull()) {
      moveCaret(editor, first.get());
    } else {
      // It's ok, do nothing.
    }
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private static void moveCaret(Editor editor, int toOffset) {
    Caret caret = editor.getCaretModel().getCurrentCaret();
    caret.removeSelection();
    caret.moveToOffset(toOffset);
    FoldingModel foldingModel = editor.getFoldingModel();
    FoldRegion region = foldingModel.getCollapsedRegionAtOffset(toOffset);
    if (region != null) {
      foldingModel.runBatchFoldingOperation(() -> region.setExpanded(true));
    }
    if (caret == editor.getCaretModel().getPrimaryCaret()) {
      editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }
  }

  protected enum Direction {
    FORWARD(Comparator.naturalOrder()),
    BACKWARD(Comparator.reverseOrder());

    public final Comparator<Integer> ordering;

    Direction(Comparator<Integer> ordering) {
      this.ordering = ordering;
    }
  }
}
