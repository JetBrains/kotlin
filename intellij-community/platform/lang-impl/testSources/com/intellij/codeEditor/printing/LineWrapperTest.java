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
import org.junit.Test;

import static org.junit.Assert.*;

public class LineWrapperTest {
  private final LineWrapper.WidthProvider myWidthProvider = new LineWrapper.WidthProvider() {
    @Override
    public double getWidth(char[] text, int start, int count, double x) {
      return count; // char width of 1 pixel for all characters
    }
  };

  @Test
  public void testEmpty() {
    doTest("", false, 0);
    doTest("", true, 0);
    doTest("", false, 1);
    doTest("", true, 1);
  }

  @Test
  public void testSingleChar() {
    doTest("a", false, 0.5,
           0);
    doTest("a", true, 0.5);
  }

  @Test
  public void testCharSplit() {
    doTest("aa", false, 1.5,
           0, 1);
    doTest("aa", true, 1.5,
           1);
  }

  @Test
  public void testExtreme() {
    doTest("aa", false, 0.5,
           0, 1);
    doTest("aa", true, 0.5,
           1);
  }

  @Test
  public void testWordSplit() {
    doTest("aa  aa  aa", false, 3.5,
           2, 4, 6, 8);
    doTest("aa  aa  aa", true, 3.5,
           2, 4, 6, 8);
  }

  private void doTest(String text, boolean atLineStart, double clipWidth, int... expectedBreaks) {
    IntArrayList actualBreaks = LineWrapper.calcBreakOffsets(text.toCharArray(), 0, text.length(), atLineStart, 0, clipWidth, myWidthProvider);
    assertArrayEquals(expectedBreaks, actualBreaks.toArray());
  }
}
