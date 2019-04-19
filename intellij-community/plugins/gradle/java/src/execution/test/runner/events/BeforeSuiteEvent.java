// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import com.intellij.execution.testframework.JavaTestLocator;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleConsoleProperties;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleSMTestProxy;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;

import static com.intellij.util.io.URLUtil.SCHEME_SEPARATOR;

/**
 * @author Vladislav.Soroka
 */
public class BeforeSuiteEvent extends AbstractTestEvent {
  public BeforeSuiteEvent(GradleTestsExecutionConsole executionConsole) {
    super(executionConsole);
  }

  @Override
  public void process(@NotNull final TestEventXmlView eventXml) throws TestEventXmlView.XmlParserException {
    final String testId = eventXml.getTestId();
    final String parentTestId = eventXml.getTestParentId();
    final String name = eventXml.getTestName();
    final String fqClassName = eventXml.getTestClassName();

    if (StringUtil.isEmpty(parentTestId)) {
      registerTestProxy(testId, getResultsViewer().getTestsRootNode());
    }
    else {
      SMTestProxy parentTest = findTestProxy(parentTestId);
      if (isHiddenTestNode(name, parentTest)) {
        registerTestProxy(testId, parentTest);
      }
      else {
        String locationUrl = findLocationUrl(null, fqClassName);
        final GradleSMTestProxy testProxy = new GradleSMTestProxy(name, true, locationUrl, null);
        testProxy.setLocator(getExecutionConsole().getUrlProvider());
        testProxy.setParentId(parentTestId);
        testProxy.setStarted();
        registerTestProxy(testId, testProxy);
      }
    }
  }

  @Override
  @NotNull
  protected String findLocationUrl(@Nullable String name, @NotNull String fqClassName) {
    return name == null
           ? JavaTestLocator.SUITE_PROTOCOL + SCHEME_SEPARATOR + fqClassName
           : JavaTestLocator.SUITE_PROTOCOL + SCHEME_SEPARATOR + StringUtil.getQualifiedName(fqClassName, name);
  }

  private boolean isHiddenTestNode(String name, SMTestProxy parentTest) {
    return parentTest != null &&
           !GradleConsoleProperties.SHOW_INTERNAL_TEST_NODES.value(getProperties()) &&
           StringUtil.startsWith(name, "Gradle Test Executor");
  }
}
