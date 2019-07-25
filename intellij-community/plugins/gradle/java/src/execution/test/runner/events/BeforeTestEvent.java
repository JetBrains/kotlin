/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.util.text.StringUtil;
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
        final List<GradleSMTestProxy> notYetAddedParents = ContainerUtil.newSmartList();
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
