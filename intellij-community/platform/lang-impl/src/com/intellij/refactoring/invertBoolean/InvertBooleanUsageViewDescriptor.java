// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.invertBoolean;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 */
public class InvertBooleanUsageViewDescriptor implements UsageViewDescriptor {
  private final PsiElement myElement;

  public InvertBooleanUsageViewDescriptor(final PsiElement element) {
    myElement = element;
  }

  @Override
  public PsiElement @NotNull [] getElements() {
    return new PsiElement[] {myElement};
  }

  @Override
  public String getProcessedElementsHeader() {
    return RefactoringBundle.message("invert.boolean.elements.header", UsageViewUtil.getType(myElement));
  }

  @NotNull
  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("invert.boolean.refs.to.invert", UsageViewBundle.getReferencesString(usagesCount, filesCount));
  }
}
