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

package com.intellij.codeInsight.unwrap;

import com.intellij.openapi.util.TextRange;

import java.util.List;
import java.util.ArrayList;

public class RangeSplitter {
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
