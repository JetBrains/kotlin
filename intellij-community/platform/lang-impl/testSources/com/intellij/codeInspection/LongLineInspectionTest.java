/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.codeInspection.longLine.LongLineInspection;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Dmitry Batkovich
 */
public class LongLineInspectionTest extends LightPlatformCodeInsightFixtureTestCase {

  public void testShortLine() {
    doTest("java");
  }

  public void testXmlLongLine() {
    doTest("xml");
  }

  public void testPlain() {
    doTest("txt");
  }

  private void doTest(final String extension) {
    myFixture.enableInspections(new LongLineInspection());
    myFixture.testHighlighting(true, false, false, getTestName(true) + "." + extension);
  }

  @Override
  protected String getTestDataPath() {
    return PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/') + "/platform/lang-impl/testData/codeInspection/longLine/";
  }
}
