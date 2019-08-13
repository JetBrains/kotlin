/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.editorActions.emacs.EmacsProcessingHandler;
import com.intellij.codeInsight.editorActions.emacs.LanguageEmacsExtension;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

public class EmacsStyleIndentAction extends BaseCodeInsightAction implements DumbAware {

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new Handler();
  }

  @Override
  protected boolean isValidForFile(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    final PsiElement context = file.findElementAt(editor.getCaretModel().getOffset());
    return context != null && LanguageFormatting.INSTANCE.forContext(context) != null;
  }

  //----------------------------------------------------------------------
  private static class Handler implements CodeInsightActionHandler {

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
      EmacsProcessingHandler emacsProcessingHandler = LanguageEmacsExtension.INSTANCE.forLanguage(file.getLanguage());
      if (emacsProcessingHandler != null) {
        EmacsProcessingHandler.Result result = emacsProcessingHandler.changeIndent(project, editor, file);
        if (result == EmacsProcessingHandler.Result.STOP) {
          return;
        }
      }

      final Document document = editor.getDocument();
      int startLine = document.getLineNumber(editor.getSelectionModel().getSelectionStart());
      int endLine = document.getLineNumber(editor.getSelectionModel().getSelectionEnd());
      for (int line = startLine; line <= endLine; line++) {
        final int lineStart = document.getLineStartOffset(line);
        final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        final int newPos = codeStyleManager.adjustLineIndent(file, lineStart);
        if (startLine == endLine && editor.getCaretModel().getOffset() < newPos) {
          editor.getCaretModel().moveToOffset(newPos);
          editor.getSelectionModel().removeSelection();
          editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
      }
    }
  }
}
