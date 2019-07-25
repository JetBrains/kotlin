// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.actions;

import com.intellij.lang.Language;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.lang.refactoring.InlineHandler;
import com.intellij.lang.refactoring.InlineHandlers;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.inline.InlineRefactoringActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InlineAction extends BasePlatformRefactoringAction {

  public InlineAction() {
    setInjectedContext(true);
  }

  @Override
  public boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  protected boolean isAvailableOnElementInEditorAndFile(@NotNull PsiElement element, @NotNull Editor editor, @NotNull PsiFile file, @NotNull DataContext context) {
    return hasInlineActionHandler(element, PsiUtilBase.getLanguageInEditor(editor, element.getProject()), editor);
  }

  @Override
  public boolean isEnabledOnElements(@NotNull PsiElement[] elements) {
    return elements.length == 1 && hasInlineActionHandler(elements [0], null, null);
  }

  private static boolean hasInlineActionHandler(PsiElement element, @Nullable Language editorLanguage, Editor editor) {
    for(InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
      if (handler.isEnabledOnElement(element, editor)) {
        return true;
      }
    }
    List<InlineHandler> handlers = InlineHandlers.getInlineHandlers(
      editorLanguage != null ? editorLanguage : element.getLanguage()
    );
    for (InlineHandler handler : handlers) {
      if (handler.canInlineElement(element)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
    return new InlineRefactoringActionHandler();
  }

  @Override
  protected RefactoringActionHandler getHandler(@NotNull Language language, PsiElement element) {
    RefactoringActionHandler handler = super.getHandler(language, element);
    if (handler != null) return handler;
    List<InlineHandler> handlers = InlineHandlers.getInlineHandlers(language);
    return handlers.isEmpty() ? null : new InlineRefactoringActionHandler();
  }

  @Override
  protected boolean isAvailableForLanguage(Language language) {
    for(InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
      if (handler.isEnabledForLanguage(language)) {
        return true;
      }
    }
    return InlineHandlers.getInlineHandlers(language).size() > 0;
  }

  @Nullable
  @Override
  protected String getActionName(@NotNull DataContext dataContext) {
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    PsiElement element = findRefactoringTargetInEditor(dataContext);
    if (element != null && editor != null) {
      for (InlineActionHandler handler: InlineActionHandler.EP_NAME.getExtensionList()) {
        if (handler.isEnabledOnElement(element, editor)) {
          return handler.getActionName(element);
        }
      }
    }

    return super.getActionName(dataContext);
  }
}
