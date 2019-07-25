// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.findUsages;

import com.intellij.find.FindManager;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageTargetProvider;
import org.jetbrains.annotations.NotNull;

public class DefaultUsageTargetProvider implements UsageTargetProvider {

  @Override
  public UsageTarget[] getTargets(@NotNull PsiElement psiElement) {
    if (psiElement instanceof NavigationItem) {
      if (FindManager.getInstance(psiElement.getProject()).canFindUsages(psiElement)) {
        return new UsageTarget[]{new PsiElement2UsageTargetAdapter(psiElement)};
      }
    }
    return null;
  }
}
