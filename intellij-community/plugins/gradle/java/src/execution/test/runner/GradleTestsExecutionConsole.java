// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.build.BuildViewSettingsProvider;
import com.intellij.execution.Platform;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class GradleTestsExecutionConsole extends SMTRunnerConsoleView implements BuildViewSettingsProvider {
  private final Map<String, SMTestProxy> testsMap = new HashMap<>();
  private final StringBuilder myBuffer = new StringBuilder();
  private boolean lastMessageWasEmptyLine;

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

  @Override
  public void print(@NotNull String s, @NotNull ConsoleViewContentType contentType) {
    if (detectUnwantedEmptyLine(s)) return;
    super.print(s, contentType);
  }

  // IJ Gradle test runner xml events protocol produces many unwanted empty strings
  // this is a workaround to avoid the trash in the console
  private boolean detectUnwantedEmptyLine(@NotNull String s) {
    if (Platform.current().lineSeparator.equals(s)) {
      if (lastMessageWasEmptyLine) return true;
      lastMessageWasEmptyLine = true;
    }
    else {
      lastMessageWasEmptyLine = false;
    }
    return false;
  }
}
