/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.refactoring.safeDelete.usageInfo;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author dsl
 */
public class SafeDeleteUsageInfo extends UsageInfo {
  private static final Logger LOG = Logger.getInstance(
    "#com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteUsageInfo");
  private final PsiElement myReferencedElement;

  public SafeDeleteUsageInfo(@NotNull PsiElement element, PsiElement referencedElement) {
    super(element);
    myReferencedElement = referencedElement;
  }

  public SafeDeleteUsageInfo(PsiElement element, PsiElement referencedElement,
                             int startOffset, int endOffset, boolean isNonCodeUsage) {
    super(element, startOffset, endOffset, isNonCodeUsage);
    myReferencedElement = referencedElement;
  }
  public PsiElement getReferencedElement() {
    return myReferencedElement;
  }
}
