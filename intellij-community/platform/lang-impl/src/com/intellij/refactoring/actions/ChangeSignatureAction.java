/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.refactoring.actions;

import com.intellij.lang.ContextAwareActionHandler;
import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChangeSignatureAction extends BasePlatformRefactoringAction {

  public ChangeSignatureAction() {
    setInjectedContext(true);
  }

  @Override
  public boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  public boolean isEnabledOnElements(@NotNull PsiElement[] elements) {
    if (elements.length == 1) {
      PsiElement member = findTargetMember(elements[0]);
      return member != null && getChangeSignatureHandler(member) != null;
    }
    return false;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@NotNull final PsiElement element, @NotNull final Editor editor, @NotNull PsiFile file, @NotNull DataContext context) {
    PsiElement targetMember = findTargetMember(element);
    if (targetMember == null) {
      final ChangeSignatureHandler targetHandler = getChangeSignatureHandler(file);
      if (targetHandler != null) {
        return true;
      }
      return false;
    }
    final ChangeSignatureHandler targetHandler = getChangeSignatureHandler(targetMember);
    if (targetHandler == null) return false;
    return true;
  }

  @Nullable
  private static PsiElement findTargetMember(@Nullable PsiElement element) {
    if (element == null) return null;
    final ChangeSignatureHandler fileHandler = getChangeSignatureHandler(element);
    if (fileHandler != null) {
      final PsiElement targetMember = fileHandler.findTargetMember(element);
      if (targetMember != null) return targetMember;
    }
    PsiReference reference = element.getReference();
    if (reference == null && element instanceof PsiNameIdentifierOwner) {
      return element;
    }
    if (reference != null) {
      return reference.resolve();
    }
    return null;
  }

  @Nullable
  @Override
  protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
    return provider.getChangeSignatureHandler();
  }

  @Nullable
  @Override
  protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider, final PsiElement element) {
    abstract class ContextAwareChangeSignatureHandler implements RefactoringActionHandler, ContextAwareActionHandler {}

    return new ContextAwareChangeSignatureHandler() {
      @Override
      public boolean isAvailableForQuickList(@NotNull Editor editor, @NotNull PsiFile file, @NotNull DataContext dataContext) {
        return findTargetMember(element) != null;
      }

      @Override
      public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        final PsiElement targetMember = findTargetMember(element);
        if (targetMember == null) {
          final ChangeSignatureHandler handler = getChangeSignatureHandler(file);
          if (handler != null) {
            final String notFoundMessage = handler.getTargetNotFoundMessage();
            if (notFoundMessage != null) {
              CommonRefactoringUtil.showErrorHint(project, editor, notFoundMessage, ChangeSignatureHandler.REFACTORING_NAME, null);
            }
          }
          return;
        }
        final ChangeSignatureHandler handler = getChangeSignatureHandler(targetMember);
        if (handler == null) return;
        handler.invoke(project, new PsiElement[]{targetMember}, dataContext);
      }

      @Override
      public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
        if (elements.length != 1) return;
        final PsiElement targetMember = findTargetMember(elements[0]);
        if (targetMember == null) return;
        final ChangeSignatureHandler handler = getChangeSignatureHandler(targetMember);
        if (handler == null) return;
        handler.invoke(project, new PsiElement[]{targetMember}, dataContext);
      }
    };
  }

  @Nullable
  private static ChangeSignatureHandler getChangeSignatureHandler(@NotNull PsiElement language) {
    RefactoringSupportProvider provider = LanguageRefactoringSupport.INSTANCE.forContext(language);
    return provider != null ? provider.getChangeSignatureHandler() : null;
  }
}
