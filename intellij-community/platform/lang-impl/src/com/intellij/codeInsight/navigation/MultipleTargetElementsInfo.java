// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ReferenceRange;
import org.jetbrains.annotations.NotNull;

class MultipleTargetElementsInfo extends BaseCtrlMouseInfo {

  MultipleTargetElementsInfo(@NotNull PsiElement elementAtPointer) {
    super(elementAtPointer);
  }

  MultipleTargetElementsInfo(@NotNull PsiElement elementAtPointer, @NotNull PsiReference reference) {
    super(elementAtPointer, ReferenceRange.getAbsoluteRanges(reference));
  }

  @Override
  public @NotNull CtrlMouseDocInfo getInfo() {
    return new CtrlMouseDocInfo(CodeInsightBundle.message("multiple.implementations.tooltip"), null);
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
