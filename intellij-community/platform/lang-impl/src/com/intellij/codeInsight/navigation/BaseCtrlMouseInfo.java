// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.intellij.codeInsight.navigation.CtrlMouseHandler.LOG;

@ApiStatus.Internal
public abstract class BaseCtrlMouseInfo implements CtrlMouseInfo {

  private final @NotNull List<@NotNull TextRange> myRanges;

  protected BaseCtrlMouseInfo(@NotNull List<@NotNull TextRange> ranges) {
    myRanges = ranges;
  }

  protected BaseCtrlMouseInfo(@NotNull PsiElement elementAtPointer) {
    this(getReferenceRanges(elementAtPointer));
  }

  @NotNull
  private static List<TextRange> getReferenceRanges(@NotNull PsiElement elementAtPointer) {
    if (!elementAtPointer.isPhysical()) return Collections.emptyList();
    int textOffset = elementAtPointer.getTextOffset();
    final TextRange range = elementAtPointer.getTextRange();
    if (range == null) {
      throw new AssertionError("Null range for " + elementAtPointer + " of " + elementAtPointer.getClass());
    }
    if (textOffset < range.getStartOffset() || textOffset < 0) {
      LOG.error("Invalid text offset " + textOffset + " of element " + elementAtPointer + " of " + elementAtPointer.getClass());
      textOffset = range.getStartOffset();
    }
    return Collections.singletonList(new TextRange(textOffset, range.getEndOffset()));
  }

  @Override
  public final @NotNull List<@NotNull TextRange> getRanges() {
    return myRanges;
  }
}
