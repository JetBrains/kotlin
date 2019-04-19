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
package com.intellij.codeEditor.printing;

import com.intellij.util.containers.IntArrayList;

public class LineWrapper {

  public static IntArrayList calcBreakOffsets(char[] text, int startOffset, int endOffset, boolean lineStart, double x, double clipX, 
                                              WidthProvider widthProvider) {
    IntArrayList breakOffsets = new IntArrayList();
    int nextOffset = startOffset;
    while (true) {
      int prevOffset = nextOffset;
      nextOffset = calcWordBreakOffset(text, nextOffset, endOffset, x, clipX, widthProvider);
      if (nextOffset == prevOffset && lineStart) {
        nextOffset = calcCharBreakOffset(text, nextOffset, endOffset, x, clipX, widthProvider);
        if (nextOffset == prevOffset) {
          nextOffset++; // extremal case when even one character doesn't fit into clip width
        }
      }
      if (nextOffset >= endOffset) {
        break;
      }
      breakOffsets.add(nextOffset);
      lineStart = true;
      x = 0;
    }
    return breakOffsets;
  }

  private static int calcCharBreakOffset(char[] text, int offset, int endOffset, double x, double clipX, WidthProvider widthProvider) {
    double newX = x;
    int breakOffset = offset;
    while (breakOffset < endOffset) {
      int nextOffset = breakOffset + 1;
      newX += widthProvider.getWidth(text, breakOffset, nextOffset - breakOffset, newX);
      if (newX > clipX) {
        return breakOffset;
      }
      breakOffset = nextOffset;
    }
    return breakOffset;
  }

  private static int calcWordBreakOffset(char[] text, int offset, int endOffset, double x, double clipX, WidthProvider widthProvider) {
    double newX = x;
    int breakOffset = offset;
    while (breakOffset < endOffset) {
      int nextOffset = getNextWordBreak(text, breakOffset, endOffset);
      newX += widthProvider.getWidth(text, breakOffset, nextOffset - breakOffset, newX);
      if (newX > clipX) {
        return breakOffset;
      }
      breakOffset = nextOffset;
    }
    return breakOffset;
  }

  private static int getNextWordBreak(char[] text, int offset, int endOffset) {
    boolean isId = Character.isJavaIdentifierPart(text[offset]);
    for (int i = offset + 1; i < endOffset; i++) {
      if (isId != Character.isJavaIdentifierPart(text[i])) {
        return i;
      }
    }
    return endOffset;
  }

  interface WidthProvider {
    double getWidth(char[] text, int start, int count, double x);
  }
}
