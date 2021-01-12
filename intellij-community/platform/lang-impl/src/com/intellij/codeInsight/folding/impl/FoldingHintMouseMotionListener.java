// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl;

import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.LightweightHint;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

class FoldingHintMouseMotionListener implements EditorMouseMotionListener {
  private final Project myProject;
  LightweightHint myCurrentHint;
  FoldRegion myCurrentFold;

  FoldingHintMouseMotionListener(Project project) {
    myProject = project;
  }

  @Override
  public void mouseMoved(@NotNull EditorMouseEvent e) {
    if (myProject.isDisposed()) return;
    LightweightHint hint = null;
    try {
      HintManager hintManager = HintManager.getInstance();
      if (hintManager != null && hintManager.hasShownHintsThatWillHideByOtherHint(false)) {
        return;
      }

      if (e.getArea() != EditorMouseEventArea.FOLDING_OUTLINE_AREA) return;

      Editor editor = e.getEditor();
      if (PsiDocumentManager.getInstance(myProject).isUncommited(editor.getDocument())) return;

      MouseEvent mouseEvent = e.getMouseEvent();
      FoldRegion fold = ((EditorEx)editor).getGutterComponentEx().findFoldingAnchorAt(mouseEvent.getX(), mouseEvent.getY());

      if (fold == null || !fold.isValid()) return;
      if (fold == myCurrentFold && myCurrentHint != null) {
        hint = myCurrentHint;
        return;
      }

      TextRange psiElementRange = EditorFoldingInfo.get(editor).getPsiElementRange(fold);
      if (psiElementRange == null) return;

      int textOffset = psiElementRange.getStartOffset();
      // There is a possible case that target PSI element's offset is less than fold region offset (e.g. complete method is
      // returned as PSI element for fold region that corresponds to java method code block). We don't want to show any hint
      // if start of the current fold region is displayed.
      Point foldStartXY = editor.visualPositionToXY(editor.offsetToVisualPosition(Math.max(textOffset, fold.getStartOffset())));
      Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
      if (visibleArea.y > foldStartXY.y) {
        if (myCurrentHint != null) {
          myCurrentHint.hide();
          myCurrentHint = null;
        }


        // We want to show a hint with the top fold region content that is above the current viewport position.
        // However, there is a possible case that complete region has a big height and only a little bottom part
        // is shown at the moment. We can't just show hint with the whole top content because it would hide actual
        // editor content, hence, we show max(2; available visual lines number) instead.
        // P.S. '2' is used here in assumption that many java methods have javadocs which first line is just '/**'.
        // So, it's not too useful to show only it even when available vertical space is not big enough.
        int availableVisualLines = EditorFragmentComponent.getAvailableVisualLinesAboveEditor(editor);
        int startVisualLine = editor.offsetToVisualPosition(textOffset).line;
        int desiredEndVisualLine = Math.max(0, editor.xyToVisualPosition(new Point(0, visibleArea.y)).line - 1);
        int endVisualLine = startVisualLine + availableVisualLines;
        if (endVisualLine > desiredEndVisualLine) {
          endVisualLine = desiredEndVisualLine;
        }

        // Show only the non-displayed top part of the target fold region
        int endOffset = editor.visualPositionToOffset(new VisualPosition(endVisualLine, 0));
        TextRange textRange = new UnfairTextRange(textOffset, endOffset);
        hint = EditorFragmentComponent.showEditorFragmentHint(editor, textRange, true, true);
        myCurrentFold = fold;
        myCurrentHint = hint;
      }
    }
    finally {
      if (hint == null) {
        if (myCurrentHint != null) {
          myCurrentHint.hide();
          myCurrentHint = null;
        }
        myCurrentFold = null;
      }
    }
  }
}
