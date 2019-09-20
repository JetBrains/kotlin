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
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author lesya
 */
public class PostFormatProcessorHelper {
  private final CommonCodeStyleSettings mySettings;
  private int myDelta;
  private TextRange myResultTextRange;

  /**
   * @deprecated Use {@link #PostFormatProcessorHelper(CommonCodeStyleSettings)} first getting correct language settings
   * with {@link CodeStyleSettings#getCommonSettings(Language)}!
   */
  @Deprecated
  public PostFormatProcessorHelper(final CodeStyleSettings rootSettings) {
    mySettings = rootSettings.getCommonSettings("");
  }

  public PostFormatProcessorHelper(final CommonCodeStyleSettings settings) {
    mySettings = settings;
  }

  public CommonCodeStyleSettings getSettings() {
    return mySettings;
  }

  public void updateResultRange(final int oldTextLength, final int newTextLength) {
    if (myResultTextRange == null) return;
    int thisChange = newTextLength - oldTextLength;
    myDelta += thisChange;
    myResultTextRange = new TextRange(myResultTextRange.getStartOffset(),
                                      myResultTextRange.getEndOffset() + thisChange);
  }

  public int mapOffset(int sourceOffset) {
    return myDelta + sourceOffset;
  }

  @NotNull
  public TextRange mapRange(@NotNull TextRange sourceRange) {
    return new TextRange(myDelta + sourceRange.getStartOffset(), myDelta + sourceRange.getEndOffset());
  }

  public boolean isElementPartlyInRange(@NotNull final PsiElement element) {
    if (myResultTextRange == null) return true;

    final TextRange elementRange = element.getTextRange();
    if (elementRange.getEndOffset() < myResultTextRange.getStartOffset()) return false;
    return elementRange.getStartOffset() <= myResultTextRange.getEndOffset();

  }

  public boolean isElementFullyInRange(final PsiElement element) {
    if (myResultTextRange == null) return true;

    final TextRange elementRange = element.getTextRange();

    return elementRange.getStartOffset() >= myResultTextRange.getStartOffset()
           && elementRange.getEndOffset() <= myResultTextRange.getEndOffset();
  }

  public static boolean isMultiline(@Nullable PsiElement statement) {
    if (statement == null) {
      return false;
    } else {
      return statement.textContains('\n');
    }
  }

  public void setResultTextRange(final TextRange resultTextRange) {
    myResultTextRange = resultTextRange;
    myDelta = 0;
  }

  public TextRange getResultTextRange() {
    return myResultTextRange;
  }
}
