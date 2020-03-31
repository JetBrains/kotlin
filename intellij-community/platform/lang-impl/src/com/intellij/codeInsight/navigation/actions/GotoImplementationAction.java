// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.navigation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.*;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GotoImplementationAction extends BaseCodeInsightAction implements CtrlMouseAction {

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new GotoImplementationHandler();
  }

  @Override
  protected boolean isValidForLookup() {
    return true;
  }

  @Override
  public void update(@NotNull final AnActionEvent event) {
    if (!DefinitionsScopedSearch.INSTANCE.hasAnyExecutors()) {
      event.getPresentation().setVisible(false);
    }
    else {
      super.update(event);
    }
  }

  @Override
  public @Nullable CtrlMouseInfo getCtrlMouseInfo(@NotNull Editor editor, @NotNull PsiFile file, int offset) {
    final PsiElement elementAtPointer = file.findElementAt(offset);
    if (elementAtPointer == null) {
      return null;
    }
    final PsiElement element = TargetElementUtil.getInstance().findTargetElement(editor, ImplementationSearcher.getFlags(), offset);
    PsiElement[] targetElements = new ImplementationSearcher() {
      @Override
      protected PsiElement @NotNull [] searchDefinitions(final PsiElement element, Editor editor) {
        final List<PsiElement> found = new ArrayList<>(2);
        DefinitionsScopedSearch.search(element, getSearchScope(element, editor)).forEach(psiElement -> {
          found.add(psiElement);
          return found.size() != 2;
        });
        return PsiUtilCore.toPsiElementArray(found);
      }
    }.searchImplementations(editor, element, offset);
    if (targetElements == null || targetElements.length == 0) {
      return null;
    }
    else if (targetElements.length > 1) {
      return new MultipleTargetElementsInfo(elementAtPointer);
    }
    else {
      Navigatable descriptor = EditSourceUtil.getDescriptor(targetElements[0]);
      if (descriptor == null || !descriptor.canNavigate()) {
        return null;
      }
      PsiElement targetElement = targetElements[0];
      if (targetElement == null || !targetElement.isPhysical()) {
        return null;
      }
      return new SingleTargetElementInfo(elementAtPointer, targetElement);
    }
  }
}
