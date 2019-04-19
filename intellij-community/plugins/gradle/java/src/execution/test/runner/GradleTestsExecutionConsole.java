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
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.build.BuildViewSettingsProvider;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class GradleTestsExecutionConsole extends SMTRunnerConsoleView implements BuildViewSettingsProvider {
  private final Map<String, SMTestProxy> testsMap = ContainerUtil.newHashMap();
  private final StringBuilder myBuffer = new StringBuilder();

  public GradleTestsExecutionConsole(TestConsoleProperties consoleProperties, @Nullable String splitterProperty) {
    super(consoleProperties, splitterProperty);
  }

  public Map<String, SMTestProxy> getTestsMap() {
    return testsMap;
  }

  public StringBuilder getBuffer() {
    return myBuffer;
  }

  @Override
  public void dispose() {
    testsMap.clear();
    super.dispose();
  }

  public SMTestLocator getUrlProvider() {
    return GradleConsoleProperties.GRADLE_TEST_LOCATOR;
  }

  @Override
  public boolean isExecutionViewHidden() {
    return Registry.is("build.view.side-by-side", true);
  }
}
