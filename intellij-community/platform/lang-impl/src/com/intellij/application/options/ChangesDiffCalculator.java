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
package com.intellij.application.options;

import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.comparison.DiffTooBigException;
import com.intellij.diff.fragments.DiffFragment;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows to calculate difference between two versions of document (before and after code style setting value change).
 */
public class ChangesDiffCalculator {
  private static final Logger LOG = Logger.getInstance(ChangesDiffCalculator.class);

  public static List<TextRange> calculateDiff(@NotNull Document beforeDocument, @NotNull Document currentDocument) {
    CharSequence beforeText = beforeDocument.getCharsSequence();
    CharSequence currentText = currentDocument.getCharsSequence();

    try {
      ComparisonManager manager = ComparisonManager.getInstance();
      List<LineFragment> lineFragments = manager.compareLinesInner(beforeText, currentText, ComparisonPolicy.DEFAULT,
                                                                   DumbProgressIndicator.INSTANCE);

      List<TextRange> modifiedRanges = new ArrayList<>();

      for (LineFragment lineFragment : lineFragments) {
        int fragmentStartOffset = lineFragment.getStartOffset2();
        int fragmentEndOffset = lineFragment.getEndOffset2();

        List<DiffFragment> innerFragments = lineFragment.getInnerFragments();
        if (innerFragments != null) {
          for (DiffFragment innerFragment : innerFragments) {
            int innerFragmentStartOffset = fragmentStartOffset + innerFragment.getStartOffset2();
            int innerFragmentEndOffset = fragmentStartOffset + innerFragment.getEndOffset2();
            modifiedRanges.add(calculateChangeHighlightRange(currentText, innerFragmentStartOffset, innerFragmentEndOffset));
          }
        }
        else {
          modifiedRanges.add(calculateChangeHighlightRange(currentText, fragmentStartOffset, fragmentEndOffset));
        }
      }

      return modifiedRanges;
    }
    catch (DiffTooBigException e) {
      LOG.info(e);
      return Collections.emptyList();
    }
  }

  /**
   * This method shifts changed range to the rightmost possible offset.
   *
   * Thus, when comparing whitespace sequences of different length, we always highlight rightmost whitespaces
   * (while general algorithm gives no warranty on this case, and usually highlights leftmost whitespaces).
   */
  @NotNull
  private static TextRange calculateChangeHighlightRange(@NotNull CharSequence text, int startOffset, int endOffset) {
    if (startOffset == endOffset) {
      while (startOffset < text.length() && text.charAt(startOffset) == ' ') {
        startOffset++;
      }
      return new TextRange(startOffset, startOffset);
    }

    int originalStartOffset = startOffset;
    int originalEndOffset = endOffset;

    while (endOffset < text.length() &&
           rangesEqual(text, originalStartOffset, originalEndOffset, startOffset + 1, endOffset + 1)) {
      startOffset++;
      endOffset++;
    }

    return new TextRange(startOffset, endOffset);
  }
  
  private static boolean rangesEqual(@NotNull CharSequence text, int start1, int end1, int start2, int end2) {
    if (end1 - start1 != end2 - start2) return false;
    for (int i = start1; i < end1; i++) {
      if (text.charAt(i) != text.charAt(i - start1 + start2)) return false;
    }
    return true;
  }
}
