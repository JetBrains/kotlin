// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.FileBasedTestCaseHelper;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.intellij.testFramework.EdtTestUtil.runInEdtAndWait;

@RunWith(com.intellij.testFramework.Parameterized.class)
@TestDataPath("/testData/../../../platform/lang-impl/testData/editor/braceHighlighterBlock/")
public class BraceHighlightingHandlerBlockCaretTest extends LightPlatformCodeInsightTestCase implements FileBasedTestCaseHelper {
  @Test
  public void testAction() {
    runInEdtAndWait(() -> {
      configureByFile(myFileSuffix);
      Editor editor = getEditor();
      editor.getSettings().setBlockCursor(true);
      BraceHighlightingHandlerTest.doTest(getProject(), getFile(), editor);
    });
  }

  @Nullable
  @Override
  public String getFileSuffix(String fileName) {
    return fileName;
  }
}