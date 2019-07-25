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

package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.HintAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptorBase;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 * Extends {@link ProblemDescriptorBase} with {@link HintAction}
 */
public class ProblemDescriptorImpl extends ProblemDescriptorBase {
  private final HintAction myHintAction;

  public ProblemDescriptorImpl(@NotNull PsiElement startElement,
                               @NotNull PsiElement endElement,
                               @NotNull String descriptionTemplate,
                               LocalQuickFix[] fixes,
                               @NotNull ProblemHighlightType highlightType,
                               boolean isAfterEndOfLine,
                               @Nullable TextRange rangeInElement,
                               boolean onTheFly) {
    this(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, null, onTheFly);
  }

  public ProblemDescriptorImpl(@NotNull PsiElement startElement,
                               @NotNull PsiElement endElement,
                               @NotNull String descriptionTemplate,
                               LocalQuickFix[] fixes,
                               @NotNull ProblemHighlightType highlightType,
                               boolean isAfterEndOfLine,
                               @Nullable TextRange rangeInElement,
                               @Nullable HintAction hintAction,
                               boolean onTheFly) {
    this(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, true, hintAction, onTheFly);
  }

  public ProblemDescriptorImpl(@NotNull PsiElement startElement,
                               @NotNull PsiElement endElement,
                               @NotNull String descriptionTemplate,
                               LocalQuickFix[] fixes,
                               @NotNull ProblemHighlightType highlightType,
                               boolean isAfterEndOfLine,
                               @Nullable TextRange rangeInElement,
                               final boolean showTooltip,
                               @Nullable HintAction hintAction,
                               boolean onTheFly) {
    super(startElement, endElement, descriptionTemplate, fixes, highlightType, isAfterEndOfLine, rangeInElement, showTooltip, onTheFly);
    myHintAction = hintAction;
  }

  public HintAction getHintAction() {
    return myHintAction;
  }
}
