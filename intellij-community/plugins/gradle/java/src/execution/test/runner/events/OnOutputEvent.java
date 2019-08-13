// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;

/**
 * @author Vladislav.Soroka
 */
public class OnOutputEvent extends AbstractTestEvent {

  public OnOutputEvent(GradleTestsExecutionConsole executionConsole) {
    super(executionConsole);
  }

  @Override
  public void process(@NotNull final TestEventXmlView eventXml) throws TestEventXmlView.XmlParserException {
    final String testId = eventXml.getTestId();
    final String destination = eventXml.getTestEventTestDescription();
    final String output = decode(eventXml.getTestEventTest());

    SMTestProxy testProxy = findTestProxy(testId);
    if (testProxy == null) return;

    testProxy.addOutput(output, "StdOut".equals(destination) ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
  }
}
