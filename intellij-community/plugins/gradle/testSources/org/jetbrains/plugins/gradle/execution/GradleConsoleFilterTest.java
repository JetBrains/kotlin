/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.execution;

import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;

/**
 * @author Vladislav.Soroka
 */
public class GradleConsoleFilterTest extends CodeInsightFixtureTestCase {

  public void testApplyFilter() {
    doTest("Build file 'C:\\project\\build.gradle' line: 7", "C:\\project\\build.gradle", 7);
    doTest("Build file '/project/build.gradle' line: 7", "/project/build.gradle", 7);
    doTest("  build file 'C:\\project\\build.gradle': 49: unexpected token: 5 @ line 49, column 28.", "C:\\project\\build.gradle", 49);
    doTest("build file 'C:\\project\\build.gradle': 49: unexpected token: 5 @ line 49, column 28.", "C:\\project\\build.gradle", 49);
    doTest("Settings file 'C:\\project\\settings.gradle' line: 7", "C:\\project\\settings.gradle", 7);
    doTest("Settings file '/project/settings.gradle' line: 7", "/project/settings.gradle", 7);
  }

  private void doTest(String line, String expectedFileName, int expectedLineNumber) {
    GradleConsoleFilter filter = new GradleConsoleFilter(myFixture.getProject());
    filter.applyFilter(line, 0);
    assertEquals(expectedFileName, filter.getFilteredFileName());
    assertEquals(expectedLineNumber, filter.getFilteredLineNumber());
  }
}