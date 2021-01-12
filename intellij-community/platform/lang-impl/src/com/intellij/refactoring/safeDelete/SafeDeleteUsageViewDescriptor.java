// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.safeDelete;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * @author dsl
 */
public class SafeDeleteUsageViewDescriptor implements UsageViewDescriptor {
  private final PsiElement[] myElementsToDelete;

  public SafeDeleteUsageViewDescriptor(PsiElement[] elementsToDelete) {
    myElementsToDelete = elementsToDelete;
  }

  @Override
  public PsiElement @NotNull [] getElements() {
    return myElementsToDelete;
  }

  @Override
  public String getProcessedElementsHeader() {
    return RefactoringBundle.message("items.to.be.deleted");
  }

  @NotNull
  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("references.in.code", UsageViewBundle.getReferencesString(usagesCount, filesCount));
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("safe.delete.comment.occurences.header",
                                     UsageViewBundle.getOccurencesString(usagesCount, filesCount));
  }
}
