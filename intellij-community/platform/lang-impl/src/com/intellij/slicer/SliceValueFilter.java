// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.slicer;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public interface SliceValueFilter {
  /**
   * @param element to test
   * @return true if this element passes the filter
   */
  boolean allowed(@NotNull PsiElement element);

  /**
   * @param element to which the filter is applied
   * @return String representation
   */
  @NotNull @Nls String getPresentationText(@NotNull PsiElement element);
}
