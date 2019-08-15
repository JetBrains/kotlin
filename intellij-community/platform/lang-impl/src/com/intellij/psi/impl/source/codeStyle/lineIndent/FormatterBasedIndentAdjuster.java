// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.codeStyle.lineIndent;

import com.intellij.formatting.FormattingMode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FormatterBasedIndentAdjuster  {

  private final static int MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE = 100000;

  private FormatterBasedIndentAdjuster() {
  }

  /**
   * Performs a delayed indent adjustment for large documents bigger than {@link #MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE} by
   * scheduling it to a time when the document is committed. Uses formatter to calculate the new indent on a background
   * thread. Only the actual change is done on EDT: the old indent is replaced with a new indent string directly in the
   * document. Doesn't commit the document, thus a subsequent {@link PsiDocumentManager#commitDocument(Document)} may
   * be required.
   * <b>Note:</b> visually it may lead to a text jump which becomes more obvious, more time it takes to calculate the
   * new indent using a formatting model. A better way to handle large documents is to implement
   * {@link com.intellij.psi.codeStyle.lineIndent.LineIndentProvider} returning a non-null value when possible.
   *
   * @param myProject  The current project.
   * @param myDocument The document to be modified.
   * @param myOffset   The offset in the line whose indent is to be adjusted.
   */
  public static void scheduleIndentAdjustment(@NotNull Project myProject,
                                              @NotNull Document myDocument,
                                              int myOffset) {
    IndentAdjusterRunnable fixer = new IndentAdjusterRunnable(myProject, myDocument, myOffset);
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
    if (isSynchronousAdjustment(myDocument)) {
      documentManager.commitDocument(myDocument);
    }
    fixer.run();
  }

  private static boolean isSynchronousAdjustment(@NotNull Document document) {
    return ApplicationManager.getApplication().isUnitTestMode() || document.getTextLength() <= MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE;
  }

  public static class IndentAdjusterRunnable implements Runnable {
    private final Project myProject;
    private final int myLine;
    private final Document myDocument;

    public IndentAdjusterRunnable(Project project, Document document, int offset) {
      myProject = project;
      myDocument = document;
      myLine = myDocument.getLineNumber(offset);
    }

    @Override
    public void run() {
      int lineStart = myDocument.getLineStartOffset(myLine);
      int indentEnd = CharArrayUtil.shiftForward(myDocument.getCharsSequence(), lineStart, " \t");
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
      if (file != null) {
        RangeMarker indentMarker = myDocument.createRangeMarker(lineStart, indentEnd);
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(myProject);
        if (isSynchronousAdjustment(myDocument)) {
          updateIndent(indentMarker, codeStyleManager.getLineIndent(file, lineStart, FormattingMode.ADJUST_INDENT_ON_ENTER));
        }
        else {
          ReadAction.nonBlocking(() -> codeStyleManager.getLineIndent(file, lineStart, FormattingMode.ADJUST_INDENT_ON_ENTER))
            .withDocumentsCommitted(myProject)
            .finishOnUiThread(ModalityState.NON_MODAL, indentString -> updateIndent(indentMarker, indentString))
            .submit(AppExecutorUtil.getAppExecutorService());
        }
      }
    }

    private void updateIndent(@NotNull RangeMarker indentMarker, @Nullable String newIndent) {
      if (newIndent != null) {
        CommandProcessor.getInstance().runUndoTransparentAction(
          () ->
            ApplicationManager.getApplication().runWriteAction(() -> {
              myDocument.replaceString(indentMarker.getStartOffset(), indentMarker.getEndOffset(), newIndent);
              indentMarker.dispose();
            }));
      }
    }

  }

}
