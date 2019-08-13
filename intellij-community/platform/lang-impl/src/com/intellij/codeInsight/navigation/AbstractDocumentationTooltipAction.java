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
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.reference.SoftReference;
import com.intellij.util.PatchedWeakReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Expands {@link AnAction} contract for documentation-related actions that may be called from the IDE tooltip.
 * 
 * @author Denis Zhdanov
 */
public abstract class AbstractDocumentationTooltipAction extends AnAction {

  @Nullable private WeakReference<PsiElement> myDocAnchor;
  @Nullable private WeakReference<PsiElement> myOriginalElement;

  public void setDocInfo(@NotNull PsiElement docAnchor, @NotNull PsiElement originalElement) {
    myDocAnchor = new PatchedWeakReference<>(docAnchor);
    myOriginalElement = new PatchedWeakReference<>(originalElement);
  }
  
  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(getDocInfo() != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Pair<PsiElement, PsiElement> info = getDocInfo();
    if (info == null) {
      return;
    }
    doActionPerformed(e.getDataContext(), info.first, info.second);
    myDocAnchor = null;
    myOriginalElement = null;
  }
  
  protected abstract void doActionPerformed(@NotNull DataContext context,
                                            @NotNull PsiElement docAnchor,
                                            @NotNull PsiElement originalElement);
  
  @Nullable
  private Pair<PsiElement/* doc anchor */, PsiElement /* original element */> getDocInfo() {
    PsiElement docAnchor = SoftReference.dereference(myDocAnchor);
    if (docAnchor == null) {
      return null;
    }
    PsiElement originalElement = SoftReference.dereference(myOriginalElement);
    if (originalElement == null) {
      return null;
    }
    return Pair.create(docAnchor, originalElement);
  }
}
