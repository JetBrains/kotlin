// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ReferenceRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public class MultipleTargetElementsInfo extends BaseCtrlMouseInfo {

  public MultipleTargetElementsInfo(@NotNull PsiElement elementAtPointer) {
    super(elementAtPointer);
  }

  public MultipleTargetElementsInfo(@NotNull PsiReference reference) {
    super(ReferenceRange.getAbsoluteRanges(reference));
  }

  @Override
  public @NotNull CtrlMouseDocInfo getDocInfo() {
    return new CtrlMouseDocInfo(CodeInsightBundle.message("multiple.implementations.tooltip"), null, null);
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
