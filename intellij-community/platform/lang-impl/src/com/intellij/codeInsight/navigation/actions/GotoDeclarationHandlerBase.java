// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class GotoDeclarationHandlerBase implements GotoDeclarationHandler {
  @Nullable
  @Override
  public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
    final PsiElement target = getGotoDeclarationTarget(sourceElement, editor);
    return target != null ? new PsiElement[]{target} : null;
  }

  @Nullable
  public abstract PsiElement getGotoDeclarationTarget(@Nullable PsiElement sourceElement, Editor editor);
}
