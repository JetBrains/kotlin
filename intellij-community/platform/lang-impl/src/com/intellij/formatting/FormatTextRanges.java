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
package com.intellij.formatting;

import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.TextRangeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class FormatTextRanges implements FormattingRangesInfo {
  private final List<TextRange>       myInsertedRanges;
  private final List<FormatTextRange> myRanges         = new ArrayList<>();
  private final List<TextRange>       myExtendedRanges = new ArrayList<>();
  private final List<TextRange>       myDisabledRanges = new ArrayList<>();

  private boolean myExtendToContext;

  public FormatTextRanges() {
    myInsertedRanges = null;
  }

  public FormatTextRanges(TextRange range, boolean processHeadingWhitespace) {
    myInsertedRanges = null;
    add(range, processHeadingWhitespace);
  }

  public FormatTextRanges(@NotNull ChangedRangesInfo changedRangesInfo, @NotNull List<TextRange> contextRanges) {
    myInsertedRanges = changedRangesInfo.insertedRanges;
    boolean processHeadingWhitespace = false;
    for (TextRange range : contextRanges) {
      add(range, processHeadingWhitespace);
      processHeadingWhitespace = true;
    }
  }

  public void add(TextRange range, boolean processHeadingWhitespace) {
    myRanges.add(new FormatTextRange(range, processHeadingWhitespace));
  }

  @Override
  public boolean isWhitespaceReadOnly(final @NotNull TextRange range) {
    return myRanges.stream().allMatch(formatTextRange -> formatTextRange.isWhitespaceReadOnly(range));
  }
  
  @Override
  public boolean isReadOnly(@NotNull TextRange range) {
    return myRanges.stream().allMatch(formatTextRange -> formatTextRange.isReadOnly(range));
  }

  @Override
  public boolean isOnInsertedLine(int offset) {
    if (myInsertedRanges == null) return false;

    Optional<TextRange> enclosingRange = myInsertedRanges.stream()
      .filter((range) -> range.contains(offset))
      .findAny();

    return enclosingRange.isPresent();
  }
  
  public List<FormatTextRange> getRanges() {
    return myRanges;
  }

  public FormatTextRanges ensureNonEmpty() {
    FormatTextRanges result = new FormatTextRanges();
    for (FormatTextRange range : myRanges) {
      if (range.isProcessHeadingWhitespace()) {
        result.add(range.getNonEmptyTextRange(), true);
      }
      else {
        result.add(range.getTextRange(), false);
      }
    }
    return result;
  }

  public boolean isEmpty() {
    return myRanges.isEmpty();
  }

  public boolean isFullReformat(PsiFile file) {
    return myRanges.size() == 1 && file.getTextRange().equals(myRanges.get(0).getTextRange());
  }

  public List<TextRange> getTextRanges() {
    List<TextRange> ranges = ContainerUtil.map(myRanges, FormatTextRange::getTextRange);
    ranges.sort(Segment.BY_START_OFFSET_THEN_END_OFFSET);
    return ranges;
  }

  public void setExtendedRanges(@NotNull List<TextRange> extendedRanges) {
    myExtendedRanges.addAll(extendedRanges);
  }
  
  public List<TextRange> getExtendedRanges() {
    return myExtendedRanges.isEmpty() ? getTextRanges() : myExtendedRanges;
  }


  /**
   * @return A range containing all ranges or null if no ranges.
   */
  @Nullable
  public TextRange getBoundRange() {
    List<TextRange> ranges = getTextRanges();
    return ranges.size() > 0 ?
           new TextRange(ranges.get(0).getStartOffset(), ranges.get(ranges.size() - 1).getEndOffset()) :
           null;
  }

  public boolean isExtendToContext() {
    return myExtendToContext;
  }

  public void setExtendToContext(boolean extendToContext) {
    myExtendToContext = extendToContext;
  }

  public void setDisabledRanges(@NotNull Collection<TextRange> disabledRanges) {
    myDisabledRanges.clear();
    myDisabledRanges.addAll(ContainerUtil.sorted(disabledRanges, Segment.BY_START_OFFSET_THEN_END_OFFSET));
  }

  public boolean isInDisabledRange(@NotNull TextRange textRange) {
    return TextRangeUtil.intersectsOneOf(textRange, myDisabledRanges);
  }
}