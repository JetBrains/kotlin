// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl;

import com.intellij.codeInsight.folding.CollapseBlockHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CollapseBlockHandlerImpl implements CollapseBlockHandler {
  Logger LOG = Logger.getInstance("#com.intellij.codeInsight.folding.CollapseBlockHandler");

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    int[] targetCaretOffset = {-1};
    editor.getFoldingModel().runBatchFoldingOperation(() -> {
      final EditorFoldingInfo info = EditorFoldingInfo.get(editor);
      FoldingModelEx model = (FoldingModelEx) editor.getFoldingModel();
      int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset - 1);
      if (!isEndBlockToken(element)) {
        element = file.findElementAt(offset);
      }
      if (element == null) return;
      PsiElement block = findParentBlock(element);
      FoldRegion previous = null;
      FoldRegion myPrevious = null;
      while (block != null) {
        TextRange range = getFoldingRange(block);
        if (!range.containsOffset(offset)) {
          block = findParentBlock(block);
          continue;
        }
        int start = range.getStartOffset();
        int end = range.getEndOffset();
        FoldRegion existing = FoldingUtil.findFoldRegion(editor, start, end);
        if (existing != null) {
          if (existing.isExpanded()) {
            existing.setExpanded(false);
            targetCaretOffset[0] = existing.getEndOffset();
            return;
          }
          previous = existing;
          if (info.getPsiElement(existing) == null) myPrevious = existing;
          block = findParentBlock(block);
          continue;
        }
        if (!model.intersectsRegion(start, end)) {
          FoldRegion region = model.addFoldRegion(start, end, getPlaceholderText());
          LOG.assertTrue(region != null);
          region.setExpanded(false);
          if (myPrevious != null && info.getPsiElement(region) == null) {
            info.removeRegion(myPrevious);
            model.removeFoldRegion(myPrevious);
          }
          targetCaretOffset[0] = block.getTextRange().getEndOffset() < offset ? start : end;
          return;
        } else break;
      }
      if (previous != null) {
        previous.setExpanded(false);
        if (myPrevious != null) {
          info.removeRegion(myPrevious);
          model.removeFoldRegion(myPrevious);
        }
        targetCaretOffset[0] = previous.getEndOffset();
      }
    });
    if (targetCaretOffset[0] >= 0) editor.getCaretModel().moveToOffset(targetCaretOffset[0]);
  }

  @Nullable
  protected abstract PsiElement findParentBlock(@Nullable PsiElement element);

  protected abstract boolean isEndBlockToken(@Nullable PsiElement element);

  @NotNull
  protected String getPlaceholderText() { return "{...}"; }

  @NotNull
  protected TextRange getFoldingRange(@NotNull PsiElement element) {
    return element.getTextRange();
  }
}
