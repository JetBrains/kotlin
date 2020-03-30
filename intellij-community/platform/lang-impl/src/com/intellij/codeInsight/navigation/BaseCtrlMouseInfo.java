// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.intellij.codeInsight.navigation.CtrlMouseHandler.LOG;

public abstract class CtrlMouseInfo {

  private final @NotNull PsiElement myElementAtPointer;
  private final @NotNull List<@NotNull TextRange> myRanges;

  public CtrlMouseInfo(@NotNull PsiElement elementAtPointer, @NotNull List<@NotNull TextRange> ranges) {
    myElementAtPointer = elementAtPointer;
    myRanges = ranges;
  }

  public CtrlMouseInfo(@NotNull PsiElement elementAtPointer) {
    this(elementAtPointer, getReferenceRanges(elementAtPointer));
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

  public final @NotNull PsiElement getElementAtPointer() {
    return myElementAtPointer;
  }

  public final @NotNull List<@NotNull TextRange> getRanges() {
    return myRanges;
  }

  public abstract @NotNull CtrlMouseDocInfo getInfo();

  public abstract boolean isValid();

  public boolean isNavigatable() {
    return true;
  }
}
