// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Advanced customization interface used in {@link TargetElementUtil} class to support specifics of various languages.
 * The exact API is not documented and is subject to change.
 * Please refer to {@link TargetElementUtil} for additional information.
 */
public abstract class TargetElementEvaluatorEx2 implements TargetElementEvaluator {
  @Nullable
  public PsiElement getNamedElement(@NotNull PsiElement element) {
    return null;
  }

  public boolean isAcceptableNamedParent(@NotNull PsiElement parent) {
    return true;
  }

  @Nullable
  public PsiElement adjustElement(Editor editor, int flags, @Nullable PsiElement element, @Nullable PsiElement contextElement) {
    return element;
  }

  @Nullable
  public PsiElement adjustTargetElement(Editor editor, int offset, int flags, @NotNull PsiElement targetElement) {
    return targetElement;
  }

  @Nullable
  public PsiElement adjustReferenceOrReferencedElement(@NotNull PsiFile file,
                                                       @NotNull Editor editor,
                                                       int offset,
                                                       int flags,
                                                       @Nullable PsiElement refElement) {
    return refElement;
  }

  @Nullable
  public PsiElement adjustReference(@NotNull PsiReference ref) {
    return null;
  }

  @Nullable
  public Collection<PsiElement> getTargetCandidates(@NotNull PsiReference reference) {
    return null;
  }

  @Nullable
  public PsiElement getGotoDeclarationTarget(@NotNull final PsiElement element, @Nullable final PsiElement navElement) {
    return null;
  }

  @NotNull
  public ThreeState isAcceptableReferencedElement(@NotNull PsiElement element, @Nullable PsiElement referenceOrReferencedElement) {
    return ThreeState.UNSURE;
  }

  @Override
  public boolean includeSelfInGotoImplementation(@NotNull final PsiElement element) {
    return true;
  }

  public boolean acceptImplementationForReference(@Nullable PsiReference reference, @NotNull PsiElement element) {
    return true;
  }

  /**
   * @return a scope where element's implementations (Goto/Show Implementations) should be searched.
   * If null is returned, default (module-with-dependents) scope will be used.
   */
  @Nullable
  public SearchScope getSearchScope(Editor editor, @NotNull PsiElement element) {
    return null;
  }
}
