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
package com.intellij.codeInsight.folding.impl;

import com.intellij.openapi.editor.FoldRegion;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;

import java.util.Iterator;

public class FoldingUtilTest extends LightPlatformCodeInsightTestCase {
  public void testFoldTreeIterator() {
    configureFromFileText(getTestName(false) + ".txt",
                          "abcdefghijklmnopqrstuvwxyz");
    EditorTestUtil.addFoldRegion(myEditor, 0, 10, ".", true);
    EditorTestUtil.addFoldRegion(myEditor, 0, 5, ".", false);
    EditorTestUtil.addFoldRegion(myEditor, 1, 2, ".", true);
    EditorTestUtil.addFoldRegion(myEditor, 7, 10, ".", false);
    EditorTestUtil.addFoldRegion(myEditor, 10, 11, ".", true);

    StringBuilder b = new StringBuilder();
    Iterator<FoldRegion> it = FoldingUtil.createFoldTreeIterator(myEditor);
    while (it.hasNext()) {
      FoldRegion region = it.next();
      if (b.length() > 0) {
        b.append('|');
      }
      b.append(region.getStartOffset()).append(',').append(region.getEndOffset());
    }

    assertEquals("0,10|0,5|1,2|7,10|10,11", b.toString());
  }
}