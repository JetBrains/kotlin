/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.impl.source;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Used by {@code PostProcessFormattingAspect} to exclude ranges which should never be automatically indented. For example, HEREDOC strings.
 *
 * @author Rustam Vishnyakov
 */
public interface DisabledIndentRangesProvider {
  ExtensionPointName<DisabledIndentRangesProvider> EP_NAME = ExtensionPointName.create("com.intellij.disabledIndentRangesProvider");

  /**
   * Collects ranges which should never be indented inside the given PSI element.
   * @param element The PSI element to check.
   * @return A collection of ranges with indentation disabled or <i>null</i> if the check is not relevant for the element in question.
   */
  @Nullable
  Collection<TextRange> getDisabledIndentRanges(@NotNull PsiElement element);
}
