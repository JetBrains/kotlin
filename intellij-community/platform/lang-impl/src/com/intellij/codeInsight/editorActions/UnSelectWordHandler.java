/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.editorActions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnSelectWordHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public UnSelectWordHandler(EditorActionHandler originalHandler) {
    super(true);
    myOriginalHandler = originalHandler;
  }

  @Override
  public void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;
    }
    Document document = editor.getDocument();
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

    if (file == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }

    PsiDocumentManager.getInstance(project).commitDocument(document);
    doAction(editor, file);
  }

  private static void doAction(@NotNull Editor editor, @NotNull PsiFile file) {
    if (!editor.getSelectionModel().hasSelection()) {
      return;
    }

    CharSequence text = editor.getDocument().getCharsSequence();

    int cursorOffset = editor.getCaretModel().getOffset();

    if (cursorOffset > 0 && cursorOffset < text.length() &&
       !Character.isJavaIdentifierPart(text.charAt(cursorOffset)) &&
       Character.isJavaIdentifierPart(text.charAt(cursorOffset - 1))) {
      cursorOffset--;
    }

    PsiElement element = file.findElementAt(cursorOffset);

    if (element instanceof PsiWhiteSpace && cursorOffset > 0) {
      PsiElement anotherElement = file.findElementAt(cursorOffset - 1);

      if (!(anotherElement instanceof PsiWhiteSpace)) {
        element = anotherElement;
      }
    }

    if (element instanceof PsiWhiteSpace) {
      PsiElement nextSibling = element.getNextSibling();
      if (nextSibling == null) {
        element = element.getParent();
        if (element == null || element instanceof PsiFile) {
          return;
        }
        nextSibling = element.getNextSibling();
        if (nextSibling == null) {
          return;
        }
      }
      element = nextSibling;
      cursorOffset = element.getTextRange().getStartOffset();
    }

    final TextRange selectionRange = new TextRange(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());

    final Ref<TextRange> maximumRange = new Ref<>();

    final int finalCursorOffset = cursorOffset;
    SelectWordUtil.processRanges(element, text, cursorOffset, editor, new Processor<TextRange>() {
      @Override
      public boolean process(TextRange range) {
        range = expandToFoldingBoundaries(range);
        if (selectionRange.contains(range) && !range.equals(selectionRange) &&
            (range.contains(finalCursorOffset) || finalCursorOffset == range.getEndOffset())) {
          if (maximumRange.get() == null || range.contains(maximumRange.get())) {
            maximumRange.set(range);
          }
        }

        return false;
      }

      private TextRange expandToFoldingBoundaries(TextRange range) {
        int startOffset = range.getStartOffset();
        FoldRegion region = editor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        if (region != null) startOffset = region.getStartOffset();
        int endOffset = range.getEndOffset();
        region = editor.getFoldingModel().getCollapsedRegionAtOffset(endOffset);
        if (region != null && endOffset > region.getStartOffset()) endOffset = region.getEndOffset();
        return new TextRange(startOffset, endOffset);
      }
    });

    TextRange range = maximumRange.get();

    if (range == null) {
      editor.getSelectionModel().setSelection(cursorOffset, cursorOffset);
    }
    else {
      editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
    }
  }
}
