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
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class FormatTextRanges implements FormattingRangesInfo {
  private final List<TextRange> myInsertedRanges;
  private final FormatRangesStorage myStorage = new FormatRangesStorage();

  public FormatTextRanges() {
    myInsertedRanges = null;
  }

  public FormatTextRanges(TextRange range, boolean processHeadingWhitespace) {
    myInsertedRanges = null;
    add(range, processHeadingWhitespace);
  }

  public FormatTextRanges(@NotNull ChangedRangesInfo changedRangesInfo) {
    List<TextRange> optimized = optimizedChangedRanges(changedRangesInfo.allChangedRanges);
    optimized.forEach((range) -> add(range, true));
    myInsertedRanges = changedRangesInfo.insertedRanges;
  }

  public void add(TextRange range, boolean processHeadingWhitespace) {
    myStorage.add(range, processHeadingWhitespace);
  }

  @Override
  public boolean isWhitespaceReadOnly(final @NotNull TextRange range) {
    return myStorage.isWhiteSpaceReadOnly(range);  
  }
  
  @Override
  public boolean isReadOnly(@NotNull TextRange range) {
    return myStorage.isReadOnly(range);
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
    return myStorage.getRanges();
  }

  public FormatTextRanges ensureNonEmpty() {
    FormatTextRanges result = new FormatTextRanges();
    for (FormatTextRange range : myStorage.getRanges()) {
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
    return myStorage.isEmpty();
  }

  public boolean isFullReformat(PsiFile file) {
    List<FormatTextRange> ranges = myStorage.getRanges();
    return ranges.size() == 1 && file.getTextRange().equals(ranges.get(0).getTextRange());
  }

  public List<TextRange> getTextRanges() {
    return ContainerUtil.map(myStorage
                               .getRanges(), FormatTextRange::getTextRange);
  }
  
  public List<TextRange> getExtendedFormattingRanges() {
    return ContainerUtil.map(myStorage
                               .getRanges(), (range) -> {
      TextRange textRange = range.getTextRange();
      return new UnfairTextRange(textRange.getStartOffset() - 500, textRange.getEndOffset() + 500);
    });
  }

  private static List<TextRange> optimizedChangedRanges(@NotNull List<TextRange> allChangedRanges) {
    if (allChangedRanges.isEmpty()) return allChangedRanges;
    List<TextRange> sorted = ContainerUtil.sorted(allChangedRanges, Segment.BY_START_OFFSET_THEN_END_OFFSET);

    List<TextRange> result = ContainerUtil.newSmartList();

    TextRange prev = sorted.get(0);
    for (TextRange next : sorted) {
      if (next.getStartOffset() <= prev.getEndOffset() + 5) {
        int newEndOffset = Math.max(prev.getEndOffset(), next.getEndOffset());
        prev = new TextRange(prev.getStartOffset(), newEndOffset);
      }
      else {
        result.add(prev);
        prev = next;
      }
    }
    result.add(prev);

    return result;
  }
  
}