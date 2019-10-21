// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.moveLeftRight;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.EditorLastActionTracker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Range;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MoveElementLeftRightActionHandler extends EditorWriteActionHandler {
  private static final Comparator<PsiElement> BY_OFFSET = Comparator.comparingInt(PsiElement::getTextOffset);

  private static final Set<String> OUR_ACTIONS =
    ContainerUtil.set(IdeActions.MOVE_ELEMENT_LEFT, IdeActions.MOVE_ELEMENT_RIGHT);

  private final boolean myIsLeft;

  public MoveElementLeftRightActionHandler(boolean isLeft) {
    super(true);
    myIsLeft = isLeft;
  }

  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    Project project = editor.getProject();
    if (project == null) return false;
    Document document = editor.getDocument();
    if (!(document instanceof DocumentEx)) return false;

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return false;
    PsiElement[] elementList = getElementList(file, caret.getSelectionStart(), caret.getSelectionEnd());
    return elementList != null;
  }

  @Nullable
  private static PsiElement[] getElementList(@NotNull PsiFile file, int rangeStart, int rangeEnd) {
    PsiElement startElement = file.findElementAt(rangeStart);
    if (startElement == null) return null;
    if (rangeEnd > rangeStart) {
      PsiElement endElement = file.findElementAt(rangeEnd - 1);
      if (endElement == null) return null;
      PsiElement element = PsiTreeUtil.findCommonParent(startElement, endElement);
      return getElementList(element, rangeStart, rangeEnd);
    }
    PsiElement[] list = getElementList(startElement, rangeStart, rangeStart);
    if (list != null || rangeStart <= 0) return list;
    startElement = file.findElementAt(rangeStart - 1);
    if (startElement == null) return null;
    return getElementList(startElement, rangeStart, rangeStart);
  }

  @Nullable
  private static PsiElement[] getElementList(PsiElement element, int rangeStart, int rangeEnd) {
    while (element != null) {
      List<MoveElementLeftRightHandler> handlers = MoveElementLeftRightHandler.EXTENSION.allForLanguageOrAny(element.getLanguage());
      for (MoveElementLeftRightHandler handler : handlers) {
        PsiElement[] elementList = handler.getMovableSubElements(element);
        if (elementList.length > 1) {
          PsiElement[] elements = elementList.clone();
          Arrays.sort(elements, BY_OFFSET);
          PsiElement first = elements[0];
          PsiElement last = elements[elements.length - 1];
          if (rangeStart >= first.getTextRange().getStartOffset() && rangeEnd <= last.getTextRange().getEndOffset() &&
              (rangeStart >= first.getTextRange().getEndOffset() || rangeEnd <= last.getTextRange().getStartOffset())) {
            return elements;
          }
        }
      }
      element = element.getParent();
    }
    return null;
  }

  @Override
  public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
    assert caret != null;
    DocumentEx document = (DocumentEx)editor.getDocument();
    Project project = editor.getProject();
    assert project != null;
    PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
    PsiFile file = psiDocumentManager.getPsiFile(document);
    assert file != null;
    int selectionStart = caret.getSelectionStart();
    int selectionEnd = caret.getSelectionEnd();
    assert selectionStart <= selectionEnd;
    PsiElement[] elementList = getElementList(file, selectionStart, selectionEnd);
    assert elementList != null;

    Range<Integer> elementRange = findRangeOfElementsToMove(elementList, selectionStart, selectionEnd);
    if (elementRange == null) return;

    if (!OUR_ACTIONS.contains(EditorLastActionTracker.getInstance().getLastActionId())) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("move.element.left.right");
    }

    int toMoveStart = elementList[elementRange.getFrom()].getTextRange().getStartOffset();
    int toMoveEnd = elementList[elementRange.getTo()].getTextRange().getEndOffset();
    int otherIndex = myIsLeft ? elementRange.getFrom() - 1 : elementRange.getTo() + 1;
    int otherStart = elementList[otherIndex].getTextRange().getStartOffset();
    int otherEnd = elementList[otherIndex].getTextRange().getEndOffset();

    selectionStart = trim(selectionStart, toMoveStart, toMoveEnd);
    selectionEnd = trim(selectionEnd, toMoveStart, toMoveEnd);
    int caretOffset = trim(caret.getOffset(), toMoveStart, toMoveEnd);

    int caretShift;
    if (toMoveStart < otherStart) {
      document.moveText(toMoveStart, toMoveEnd, otherStart);
      document.moveText(otherStart, otherEnd, toMoveStart);
      caretShift = otherEnd - toMoveEnd;
    }
    else {
      document.moveText(otherStart, otherEnd, toMoveStart);
      document.moveText(toMoveStart, toMoveEnd, otherStart);
      caretShift = otherStart - toMoveStart;
    }
    caret.moveToOffset(caretOffset + caretShift);
    caret.setSelection(selectionStart + caretShift, selectionEnd + caretShift);
  }

  @Nullable
  private Range<Integer> findRangeOfElementsToMove(@NotNull PsiElement[] elements, int startOffset, int endOffset) {
    int startIndex = elements.length;
    int endIndex = -1;
    if (startOffset == endOffset) {
      for (int i = 0; i < elements.length; i++) {
        if (elements[i].getTextRange().containsOffset(startOffset)) {
          startIndex = endIndex = i;
          break;
        }
      }
    }
    else {
      for (int i = 0; i < elements.length; i++) {
        PsiElement psiElement = elements[i];
        TextRange range = psiElement.getTextRange();
        if (i < startIndex && startOffset < range.getEndOffset()) startIndex = i;
        if (endOffset > range.getStartOffset()) endIndex = i; else break;
      }
    }
    return startIndex > endIndex || (myIsLeft ? startIndex == 0 : endIndex == elements.length - 1)
           ? null
           : new Range<>(startIndex, endIndex);
  }

  private static int trim(int offset, int rangeStart, int rangeEnd) {
    return Math.max(rangeStart, Math.min(rangeEnd, offset));
  }
}
