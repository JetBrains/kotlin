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
package com.intellij.refactoring.rename.inplace;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MemberInplaceRenameHandler extends VariableInplaceRenameHandler {
  @Override
  protected boolean isAvailable(@Nullable PsiElement element,
                                @NotNull Editor editor,
                                @NotNull PsiFile file) {
    PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());
    if (nameSuggestionContext == null && editor.getCaretModel().getOffset() > 0) {
      nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset() - 1);
    }

    if (element == null && LookupManager.getActiveLookup(editor) != null) {
      element = PsiTreeUtil.getParentOfType(nameSuggestionContext, PsiNamedElement.class);
    }
    final RefactoringSupportProvider
      supportProvider = element == null ? null : LanguageRefactoringSupport.INSTANCE.forLanguage(element.getLanguage());
    return editor.getSettings().isVariableInplaceRenameEnabled()
           && supportProvider != null
           && element instanceof PsiNameIdentifierOwner
           && supportProvider.isMemberInplaceRenameAvailable(element, nameSuggestionContext);
  }

  @Override
  public InplaceRefactoring doRename(@NotNull PsiElement elementToRename,
                                     @NotNull Editor editor,
                                     @Nullable DataContext dataContext) {
    if (elementToRename instanceof PsiNameIdentifierOwner) {
      final RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(elementToRename);
      if (processor.isInplaceRenameSupported()) {
        final StartMarkAction startMarkAction = StartMarkAction.canStart(elementToRename.getProject());
        if (startMarkAction == null || processor.substituteElementToRename(elementToRename, editor) == elementToRename) {
          processor.substituteElementToRename(elementToRename, editor, new Pass<PsiElement>() {
            @Override
            public void pass(PsiElement element) {
              final MemberInplaceRenamer renamer = createMemberRenamer(element, (PsiNameIdentifierOwner)elementToRename, editor);
              boolean startedRename = renamer.performInplaceRename();
              if (!startedRename) {
                performDialogRename(elementToRename, editor, dataContext, renamer.myInitialName);
              }
            }
          });
          return null;
        }
        else {
          final InplaceRefactoring inplaceRefactoring = editor.getUserData(InplaceRefactoring.INPLACE_RENAMER);
          if (inplaceRefactoring != null && inplaceRefactoring.getClass() == MemberInplaceRenamer.class) {
            final TemplateState templateState = TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(editor));
            if (templateState != null) {
              templateState.gotoEnd(true);
            }
          }
        }
      }
    }
    performDialogRename(elementToRename, editor, dataContext, null);
    return null;
  }

  @NotNull
  protected MemberInplaceRenamer createMemberRenamer(@NotNull PsiElement element,
                                                     @NotNull PsiNameIdentifierOwner elementToRename,
                                                     @NotNull Editor editor) {
    return new MemberInplaceRenamer(elementToRename, element, editor);
  }
}
