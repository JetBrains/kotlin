// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.focusMode;

import com.intellij.openapi.util.Segment;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ApiStatus.Experimental
public interface FocusModeProvider {
  /**
   * Please return ranges in a order from the top to the bottom.
   * <br>
   * For example:
   * <pre>(() ()) (() () ()) </pre>
   * @see FocusModePassFactory#calcFocusZones
   */
  @NotNull
  List<? extends Segment> calcFocusZones(@NotNull PsiFile file);
}
