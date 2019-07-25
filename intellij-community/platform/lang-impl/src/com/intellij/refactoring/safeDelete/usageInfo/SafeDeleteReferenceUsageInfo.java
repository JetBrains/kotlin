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

import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;

/**
 * @author dsl
 */
public abstract class SafeDeleteReferenceUsageInfo extends SafeDeleteUsageInfo implements SafeDeleteCustomUsageInfo {
  protected final boolean mySafeDelete;

  public boolean isSafeDelete() {
    return !isNonCodeUsage && mySafeDelete;
  }

  public abstract void deleteElement() throws IncorrectOperationException;

  public SafeDeleteReferenceUsageInfo(PsiElement element, PsiElement referencedElement,
                                      int startOffset, int endOffset, boolean isNonCodeUsage, boolean isSafeDelete) {
    super(element, referencedElement, startOffset, endOffset, isNonCodeUsage);
    mySafeDelete = isSafeDelete;
  }

  public SafeDeleteReferenceUsageInfo(PsiElement element, PsiElement referencedElement, boolean safeDelete) {
    super(element, referencedElement);
    mySafeDelete = safeDelete;
  }

  @Override
  public void performRefactoring() throws IncorrectOperationException {
    if (isSafeDelete()) {
      deleteElement();
    }
  }
}
