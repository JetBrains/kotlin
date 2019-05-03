// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.integrations.maven.codeInsight.actions;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.jetbrains.idea.maven.model.MavenId;

import java.io.File;
import java.util.Arrays;

/**
 * @author Vladislav.Soroka
 */
public class AddGradleDslDependencyActionTest extends LightPlatformCodeInsightFixtureTestCase {

  public void testAddMavenDependencyInEmptyFile() {
    AddGradleDslDependencyAction.TEST_THREAD_LOCAL.set(Arrays.asList(new MavenId("testGroupId", "testArtifactId", "1.0")));
    doTest("testAddMavenDependencyInEmptyFile.gradle");
  }

  public void testAddMavenDependencyIntoExistingBlock() {
    AddGradleDslDependencyAction.TEST_THREAD_LOCAL.set(Arrays.asList(new MavenId("testGroupId", "testArtifactId", "1.0")));
    doTest("testAddMavenDependencyIntoExistingBlock.gradle");
  }

  private void doTest(String file) {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> CodeInsightTestUtil.doActionTest(new AddGradleDslDependencyAction(), file, myFixture));
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected String getBasePath() {
    return "/plugins/gradle/java/testData";
  }

  @Override
  protected String getTestDataPath() {
    return PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/') + getBasePath();
  }
}
