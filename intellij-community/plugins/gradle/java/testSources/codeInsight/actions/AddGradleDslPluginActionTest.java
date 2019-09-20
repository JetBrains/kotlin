/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.codeInsight.actions;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class AddGradleDslPluginActionTest extends BasePlatformTestCase {

  public void testApplyPlugin() {
    doTest("testApplyPlugin.gradle", "java");
  }

  public void testApplyPluginInsideClosure() {
    myFixture.configureByFile("testApplyPluginInsideClosure.gradle");
    doTest("testApplyPluginInsideClosure.gradle", "java");
  }

  private void doTest(String file, String pluginName) {
    AddGradleDslPluginAction.TEST_THREAD_LOCAL.set(pluginName);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> CodeInsightTestUtil.doActionTest(new AddGradleDslPluginAction(), file, myFixture));
  }

  @Override
  protected String getBasePath() {
    return "/plugins/gradle/testData";
  }

  @Override
  protected String getTestDataPath() {
    return PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/') + getBasePath();
  }
}
