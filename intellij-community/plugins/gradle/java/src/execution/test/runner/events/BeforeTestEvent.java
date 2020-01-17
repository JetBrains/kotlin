// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleSMTestProxy;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class BeforeTestEvent extends AbstractTestEvent {

  public BeforeTestEvent(GradleTestsExecutionConsole executionConsole) {
    super(executionConsole);
  }

  @Override
  public void process(@NotNull final TestEventXmlView eventXml) throws TestEventXmlView.XmlParserException {
    final String testId = eventXml.getTestId();
    final String parentTestId = eventXml.getTestParentId();
    final String name = eventXml.getTestName();
    final String fqClassName = eventXml.getTestClassName();

    String locationUrl = findLocationUrl(name, fqClassName);
    final GradleSMTestProxy testProxy = new GradleSMTestProxy(name, false, locationUrl, fqClassName);

    testProxy.setStarted();
    testProxy.setLocator(getExecutionConsole().getUrlProvider());
    registerTestProxy(testId, testProxy);

    if (StringUtil.isEmpty(parentTestId)) {
      getResultsViewer().getTestsRootNode().addChild(testProxy);
    }
    else {
      final SMTestProxy parentTestProxy = findTestProxy(parentTestId);
      if (parentTestProxy != null) {
        final List<GradleSMTestProxy> notYetAddedParents = new SmartList<>();
        SMTestProxy currentParentTestProxy = parentTestProxy;
        while (currentParentTestProxy instanceof GradleSMTestProxy) {
          final String parentId = ((GradleSMTestProxy)currentParentTestProxy).getParentId();
          if (currentParentTestProxy.getParent() == null && parentId != null) {
            notYetAddedParents.add((GradleSMTestProxy)currentParentTestProxy);
          }
          currentParentTestProxy = findTestProxy(parentId);
        }

        for (GradleSMTestProxy gradleSMTestProxy : ContainerUtil.reverse(notYetAddedParents)) {
          final SMTestProxy parentTestProxy1 = findTestProxy(gradleSMTestProxy.getParentId());
          if (parentTestProxy1 != null) {
            parentTestProxy1.addChild(gradleSMTestProxy);
            getResultsViewer().onSuiteStarted(gradleSMTestProxy);
          }
        }
        parentTestProxy.addChild(testProxy);
      }
    }

    getResultsViewer().onTestStarted(testProxy);
  }
}
