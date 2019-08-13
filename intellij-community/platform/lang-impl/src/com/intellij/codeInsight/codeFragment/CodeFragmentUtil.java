/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.codeInsight.codeFragment;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author oleg
 */
public class CodeFragmentUtil {
  public static Position getPosition(@NotNull final PsiElement element, final int startOffset, final int endOffset) {
    final int offset = element.getTextOffset();
    if (offset < startOffset) {
      return Position.BEFORE;
    }
    if (element.getTextOffset() < endOffset) {
      return Position.INSIDE;
    }
    return Position.AFTER;
  }

  public static boolean elementFit(final PsiElement element, final int start, final int end) {
    return element != null && start <= element.getTextOffset() && element.getTextOffset() + element.getTextLength() <= end;
  }
}
