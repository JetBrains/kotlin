/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.refactoring.safeDelete;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author dsl
 */
class UsageHolder {
  private final SmartPsiElementPointer myElementPointer;
  private int myUnsafeUsages;
  private int myNonCodeUnsafeUsages;

  UsageHolder(PsiElement element, UsageInfo[] usageInfos) {
    Project project = element.getProject();
    myElementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element);

    for (UsageInfo usageInfo : usageInfos) {
      if (!(usageInfo instanceof SafeDeleteReferenceUsageInfo)) continue;
      final SafeDeleteReferenceUsageInfo usage = (SafeDeleteReferenceUsageInfo)usageInfo;
      if (usage.getReferencedElement() != element) continue;

      if (!usage.isSafeDelete()) {
        myUnsafeUsages++;
        if (usage.isNonCodeUsage || isInGeneratedCode(usage, project)) {
          myNonCodeUnsafeUsages++;
        }
      }
    }
  }

  private static boolean isInGeneratedCode(SafeDeleteReferenceUsageInfo usage, Project project) {
    VirtualFile file = usage.getVirtualFile();
    return file != null && GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project);
  }

  @NotNull
  public String getDescription() {
    final PsiElement element = myElementPointer.getElement();
    String message = RefactoringBundle.message("0.has.1.usages.that.are.not.safe.to.delete", RefactoringUIUtil.getDescription(element, true), myUnsafeUsages);
    if (myNonCodeUnsafeUsages > 0) {
      message += "<br>" + RefactoringBundle.message("safe.delete.of.those.0.in.comments.strings.non.code", myNonCodeUnsafeUsages);
    }
    return message;
  }

  public boolean hasUnsafeUsagesInCode() {
    return myUnsafeUsages != myNonCodeUnsafeUsages;
  }
}
