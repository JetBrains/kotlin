// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.inline;

import com.intellij.ide.DataManager;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.lang.refactoring.InlineHandler;
import com.intellij.lang.refactoring.InlineHandlers;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InlineRefactoringActionHandler implements RefactoringActionHandler {
  private static final Logger LOG = Logger.getInstance(InlineRefactoringActionHandler.class);
  private static final String REFACTORING_NAME = RefactoringBundle.message("inline.title");

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    LOG.assertTrue(elements.length == 1);
    if (dataContext == null) {
      dataContext = DataManager.getInstance().getDataContext();
    }
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    for(InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
      if (handler.canInlineElement(elements[0])) {
        handler.inlineElement(project, editor, elements [0]);
        return;
      }
    }

    invokeInliner(editor, elements[0]);
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file, DataContext dataContext) {
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (element == null) {
      element = BaseRefactoringAction.getElementAtCaret(editor, file);
    }
    if (element != null) {
      for(InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
        if (handler.canInlineElementInEditor(element, editor)) {
          handler.inlineElement(project, editor, element);
          return;
        }
      }

      if (invokeInliner(editor, element)) return;

      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.method.or.local.name"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, null);
    }
  }

  public static boolean invokeInliner(@Nullable Editor editor, PsiElement element) {
    final List<InlineHandler> handlers = InlineHandlers.getInlineHandlers(element.getLanguage());
    for (InlineHandler handler : handlers) {
      if (GenericInlineHandler.invoke(element, editor, handler)) {
        return true;
      }
    }
    return false;
  }
}
