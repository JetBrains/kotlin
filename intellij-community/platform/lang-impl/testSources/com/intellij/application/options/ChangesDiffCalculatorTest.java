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
package com.intellij.application.options;

import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.LightPlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ChangesDiffCalculatorTest extends LightPlatformTestCase {
  public void testEmpty() {
    doTest("", "");

    doTest("a b c", "a b c");

    doTest("a\nb\nc\n", "a\nb\nc\n");
  }

  public void testSimple() {
    doTest("", "X",
           new TextRange(0, 1));

    doTest("a B c", "a X c",
           new TextRange(2, 3));

    doTest("while()", "while ()",
           new TextRange(5, 6));

    doTest("else {", "else\n    {",
           new TextRange(4, 8));

    doTest("while ()", "while()",
           new TextRange(5, 5));

    doTest("if\n  then", "if\n\n  then",
           new TextRange(3, 4));
  }

  public void testWhitespaceSequences() {
    doTest("X     Y", "X  Y",
           new TextRange(3, 3));

    doTest("X  Y", "X     Y",
           new TextRange(3, 6));
  }

  private static void doTest(@NotNull String before, @NotNull String current, @NotNull TextRange... expectedRanges) {
    DocumentImpl beforeDocument = new DocumentImpl(before);
    DocumentImpl currentDocument = new DocumentImpl(current);

    List<TextRange> actualRanges = ChangesDiffCalculator.calculateDiff(beforeDocument, currentDocument);

    assertEquals(Arrays.asList(expectedRanges), actualRanges);
  }
}
