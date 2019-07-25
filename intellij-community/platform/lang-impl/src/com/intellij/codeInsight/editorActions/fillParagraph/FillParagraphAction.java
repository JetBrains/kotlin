package com.intellij.codeInsight.editorActions.fillParagraph;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * User : ktisha
 *
 * Action to re-flow paragraph to fit right margin.
 * Glues paragraph and then splits into lines with appropriate length
 *
 * The action came from Emacs users // PY-4775
 */
public class FillParagraphAction extends BaseCodeInsightAction implements DumbAware {

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new Handler();
  }
  private static class Handler implements CodeInsightActionHandler {

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {

      ParagraphFillHandler paragraphFillHandler = LanguageFillParagraphExtension.INSTANCE.forLanguage(file.getLanguage());

      final int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset);
      if (element != null && paragraphFillHandler != null && paragraphFillHandler.isAvailableForFile(file)
          && paragraphFillHandler.isAvailableForElement(element)) {

        paragraphFillHandler.performOnElement(element, editor);
      }
    }
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    final ParagraphFillHandler handler =
      LanguageFillParagraphExtension.INSTANCE.forLanguage(file.getLanguage());
    return handler != null && handler.isAvailableForFile(file);
  }

}
