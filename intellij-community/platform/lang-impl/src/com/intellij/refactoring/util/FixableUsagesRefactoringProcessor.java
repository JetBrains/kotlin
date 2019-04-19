// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class FixableUsagesRefactoringProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(FixableUsagesRefactoringProcessor.class);

  protected FixableUsagesRefactoringProcessor(Project project) {
    super(project);
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usageInfos) {
    CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usageInfos);
    for (UsageInfo usageInfo : usageInfos) {
      if (usageInfo instanceof FixableUsageInfo) {
        try {
          ((FixableUsageInfo)usageInfo).fixUsage();
        }
        catch (IncorrectOperationException e) {
          LOG.info(e);
        }
      }
    }
  }


  @Override
  @NotNull
  protected final UsageInfo[] findUsages() {
    final List<FixableUsageInfo> usages = Collections.synchronizedList(new ArrayList<FixableUsageInfo>());
    findUsages(usages);
    final int numUsages = usages.size();
    final FixableUsageInfo[] usageArray = usages.toArray(new FixableUsageInfo[numUsages]);
    return usageArray;
  }

  protected abstract void findUsages(@NotNull List<FixableUsageInfo> usages);

  protected static void checkConflicts(final Ref<UsageInfo[]> refUsages, final MultiMap<PsiElement,String> conflicts) {
    for (UsageInfo info : refUsages.get()) {
      final String conflict = ((FixableUsageInfo)info).getConflictMessage();
      if (conflict != null) {
        conflicts.putValue(info.getElement(), conflict);
      }
    }
  }
}
