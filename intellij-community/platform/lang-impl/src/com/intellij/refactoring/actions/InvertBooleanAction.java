// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.invertBoolean.InvertBooleanDelegate;
import com.intellij.refactoring.invertBoolean.InvertBooleanHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 */
public class InvertBooleanAction extends BaseRefactoringAction {
  @Override
  protected boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  protected boolean isEnabledOnElements(PsiElement @NotNull [] elements) {
    if (elements.length == 1 && elements[0] != null) {
      for (InvertBooleanDelegate delegate : InvertBooleanDelegate.EP_NAME.getExtensionList()) {
        if (delegate.isVisibleOnElement(elements[0])) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@NotNull final PsiElement element,
                                                        @NotNull final Editor editor,
                                                        @NotNull PsiFile file,
                                                        @NotNull DataContext context) {
    for (InvertBooleanDelegate delegate : InvertBooleanDelegate.EP_NAME.getExtensionList()) {
      if (delegate.isAvailableOnElement(element)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
    return new InvertBooleanHandler();
  }
}
