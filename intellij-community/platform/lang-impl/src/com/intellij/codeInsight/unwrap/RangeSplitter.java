// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.unwrap;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;

public final class RangeSplitter {
  public static List<TextRange> split(TextRange target, List<? extends TextRange> deviders) {
    List<TextRange> result = new ArrayList<>();
    result.add(target);

    for (TextRange devider : deviders) {
      List<TextRange> temp = new ArrayList<>();
      for (TextRange range : result) {
        if (!range.contains(devider)) {
          temp.add(range);
          continue;
        }

        if (range.getStartOffset() < devider.getStartOffset())
          temp.add(new TextRange(range.getStartOffset(), devider.getStartOffset()));

        if (range.getEndOffset() > devider.getEndOffset())
          temp.add(new TextRange(devider.getEndOffset(), range.getEndOffset()));
      }
      result = temp;
    }

    return result;
  }
}
