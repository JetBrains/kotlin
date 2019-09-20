/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.formatting;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

public class FormatTextRange {
  private @NotNull TextRange formattingRange;
  private final boolean processHeadingWhitespace;

  public FormatTextRange(@NotNull TextRange range, boolean processHeadingSpace) {
    formattingRange = range;
    processHeadingWhitespace = processHeadingSpace;
  }
  
  public boolean isProcessHeadingWhitespace() {
    return processHeadingWhitespace;
  }

  public boolean isWhitespaceReadOnly(@NotNull TextRange range) {
    if (range.getStartOffset() >= formattingRange.getEndOffset()) return true;
    
    if (processHeadingWhitespace && range.getEndOffset() == formattingRange.getStartOffset()) {
      return false;
    }
    
    return range.getEndOffset() <= formattingRange.getStartOffset();
  }

  public int getStartOffset() {
    return formattingRange.getStartOffset();
  }

  public boolean isReadOnly(@NotNull TextRange range) {
    return range.getStartOffset() > formattingRange.getEndOffset() || range.getEndOffset() < formattingRange.getStartOffset();
  }

  @NotNull
  public TextRange getTextRange() {
    return formattingRange;
  }

  public void setTextRange(@NotNull TextRange range) {
    formattingRange = range;
  }

  public TextRange getNonEmptyTextRange() {
    int endOffset = formattingRange.getStartOffset() == formattingRange.getEndOffset()
                 ? formattingRange.getEndOffset() + 1
                 : formattingRange.getEndOffset();
    
    return new TextRange(formattingRange.getStartOffset(), endOffset);
  }

  @Override
  public String toString() {
    return formattingRange.toString() + (processHeadingWhitespace ? "+" : "");
  }
}