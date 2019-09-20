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
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vladislav.Soroka
 */
public class AfterTestEvent extends AbstractTestEvent {
  public AfterTestEvent(GradleTestsExecutionConsole executionConsole) {
    super(executionConsole);
  }

  @Override
  public void process(@NotNull final TestEventXmlView eventXml) throws TestEventXmlView.XmlParserException {

    final String testId = eventXml.getTestId();

    final String startTime = eventXml.getEventTestResultStartTime();
    final String endTime = eventXml.getEventTestResultEndTime();
    final String exceptionMsg = decode(eventXml.getEventTestResultErrorMsg());
    final String stackTrace = decode(eventXml.getEventTestResultStackTrace());

    final SMTestProxy testProxy = findTestProxy(testId);
    if (testProxy == null) return;

    try {
      testProxy.setDuration(Long.valueOf(endTime) - Long.valueOf(startTime));
    }
    catch (NumberFormatException ignored) {
    }

    final TestEventResult result = TestEventResult.fromValue(eventXml.getTestEventResultType());
    switch (result) {
      case SUCCESS:
        testProxy.setFinished();
        break;
      case FAILURE:
        final String failureType = eventXml.getEventTestResultFailureType();
        if ("comparison".equals(failureType)) {
          String actualText = decode(eventXml.getEventTestResultActual());
          String expectedText = decode(eventXml.getEventTestResultExpected());
          final Condition<String> emptyString = StringUtil::isEmpty;
          String filePath = ObjectUtils.nullizeByCondition(decode(eventXml.getEventTestResultFilePath()), emptyString);
          String actualFilePath = ObjectUtils.nullizeByCondition(
            decode(eventXml.getEventTestResultActualFilePath()), emptyString);
          testProxy.setTestComparisonFailed(exceptionMsg, stackTrace, actualText, expectedText, filePath, actualFilePath, true);
        }
        else {
          Couple<String> comparisonPair =
            parseComparisonMessage(exceptionMsg, "\nExpected: is \"(.*)\"\n\\s*got: \"(.*)\"\n");
          if (comparisonPair == null) {
            comparisonPair = parseComparisonMessage(exceptionMsg, "\nExpected: is \"(.*)\"\n\\s*but: was \"(.*)\"");
          }
          if (comparisonPair == null) {
            comparisonPair = parseComparisonMessage(exceptionMsg, "\nExpected: (.*)\n\\s*got: (.*)");
          }
          if (comparisonPair == null) {
            comparisonPair = parseComparisonMessage(exceptionMsg, "\\s*expected same:<(.*)> was not:<(.*)>");
          }
          if (comparisonPair == null) {
            comparisonPair = parseComparisonMessage(exceptionMsg, ".*\\s*expected:<(.*)> but was:<(.*)>");
          }
          if (comparisonPair == null) {
            comparisonPair = parseComparisonMessage(exceptionMsg, "\nExpected: \"(.*)\"\n\\s*but: was \"(.*)\"");
          }

          if (comparisonPair != null) {
            testProxy.setTestComparisonFailed(exceptionMsg, stackTrace, comparisonPair.second, comparisonPair.first);
          }
          else {
            testProxy.setTestFailed(exceptionMsg, stackTrace, "error".equals(failureType));
          }
        }
        getResultsViewer().onTestFailed(testProxy);
        break;
      case SKIPPED:
        testProxy.setTestIgnored(null, null);
        getResultsViewer().onTestIgnored(testProxy);
        break;
      case UNKNOWN_RESULT:
        break;
    }

    getResultsViewer().onTestFinished(testProxy);
  }

  private static Couple<String> parseComparisonMessage(String message, final String regex) {
    final Matcher matcher = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
    if (matcher.matches()) {
      return Couple.of(matcher.group(1).replaceAll("\\\\n", "\n"), matcher.group(2).replaceAll("\\\\n", "\n"));
    }
    return null;
  }
}
