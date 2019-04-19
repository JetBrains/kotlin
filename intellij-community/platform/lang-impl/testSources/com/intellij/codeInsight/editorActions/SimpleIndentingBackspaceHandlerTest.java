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
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;

public class SimpleIndentingBackspaceHandlerTest extends LightPlatformCodeInsightTestCase {
  public void testBasicUnindent() {
    doTest("       <caret>text",
           "    <caret>text");
  }

  public void testAtLineStart() {
    doTest("line1\n<caret>line2",
           "line1<caret>line2");
  }
  
  public void testDeletingTabWhenIndentSizeIsSmaller() {
    CodeStyleSettings settings = new CodeStyleSettings();
    CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions();
    assertNotNull(indentOptions);
    indentOptions.INDENT_SIZE = 2;
    indentOptions.TAB_SIZE = 4;
    CodeStyleSettingsManager.getInstance(getProject()).setTemporarySettings(settings);
    try {
      doTest("\t<caret>text",
             "  <caret>text");
    }
    finally {
      CodeStyleSettingsManager.getInstance(getProject()).dropTemporarySettings();
    }
  }

  private void doTest(String before, String after) {
    SmartBackspaceMode savedMode = CodeInsightSettings.getInstance().getBackspaceMode();
    try {
      CodeInsightSettings.getInstance().setBackspaceMode(SmartBackspaceMode.INDENT);
      configureFromFileText(getTestName(false) + ".txt", before);
      executeAction(IdeActions.ACTION_EDITOR_BACKSPACE);
      checkResultByText(after);
    }
    finally {
      CodeInsightSettings.getInstance().setBackspaceMode(savedMode);
    }
  }
}
