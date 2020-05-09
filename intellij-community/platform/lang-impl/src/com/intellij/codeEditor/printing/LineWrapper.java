// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class LineWrapper {
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
