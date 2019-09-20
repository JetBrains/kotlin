// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import com.intellij.refactoring.introduceParameterObject.IntroduceParameterObjectClassDescriptor;
import com.intellij.refactoring.introduceParameterObject.IntroduceParameterObjectDelegate;
import org.jetbrains.annotations.NotNull;

public class IntroduceParameterObjectAction extends BaseRefactoringAction {

  @Override
  protected boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  protected boolean isEnabledOnElements(@NotNull final PsiElement[] elements) {
    if (elements.length == 1) {
      final IntroduceParameterObjectDelegate delegate = IntroduceParameterObjectDelegate.findDelegate(elements[0]);
      if (delegate != null && delegate.isEnabledOn(elements[0])) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@NotNull PsiElement element,
                                                        @NotNull Editor editor,
                                                        @NotNull PsiFile file,
                                                        @NotNull DataContext context,
                                                        @NotNull String place) {
    if (BaseRefactoringAction.getPsiElementArray(context).length == 0) return false;  // see BaseRefactoringAction.actionPerformed
    final IntroduceParameterObjectDelegate delegate = IntroduceParameterObjectDelegate.findDelegate(element);
    return delegate != null && delegate.isEnabledOn(element);
  }

  @Override
  protected RefactoringActionHandler getHandler(@NotNull DataContext context) {
    final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
    if (element == null) {
      return null;
    }
    final IntroduceParameterObjectDelegate<PsiNamedElement, ParameterInfo, IntroduceParameterObjectClassDescriptor<PsiNamedElement, ParameterInfo>>
      delegate = IntroduceParameterObjectDelegate.findDelegate(element);
    return delegate != null ? delegate.getHandler(element) : null;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    ExtractSuperActionBase.removeFirstWordInMainMenu(this, e);
  }
}
