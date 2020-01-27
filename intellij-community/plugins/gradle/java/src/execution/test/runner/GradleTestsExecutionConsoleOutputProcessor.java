/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.test.runner.events.*;

/**
 * Created by eugene.petrenko@gmail.com
 */
public class GradleTestsExecutionConsoleOutputProcessor {
  private static final Logger LOG = Logger.getInstance(GradleTestsExecutionConsoleOutputProcessor.class);
  @SuppressWarnings("HardCodedStringLiteral")
  private static final String LOG_EOL = "<ijLogEol/>";
  @SuppressWarnings("HardCodedStringLiteral")
  private static final String LOG_START = "<ijLog>";
  @SuppressWarnings("HardCodedStringLiteral")
  private static final String LOG_END = "</ijLog>";

  public static void onOutput(@NotNull GradleTestsExecutionConsole executionConsole,
                              @NotNull String text,
                              @NotNull Key<?> processOutputType) {
    String eventMessage = getEventMessage(executionConsole, text, processOutputType);
    if (eventMessage == null) return;

    try {
      final TestEventXmlView xml = new TestEventXPPXmlView(eventMessage);

      final TestEventType eventType = TestEventType.fromValue(xml.getTestEventType());
      TestEvent testEvent = null;
      switch (eventType) {
        case CONFIGURATION_ERROR:
          testEvent = new ConfigurationErrorEvent(executionConsole);
          break;
        case REPORT_LOCATION:
          testEvent = new ReportLocationEvent(executionConsole);
          break;
        case BEFORE_TEST:
          testEvent = new BeforeTestEvent(executionConsole);
          break;
        case ON_OUTPUT:
          testEvent = new OnOutputEvent(executionConsole);
          break;
        case AFTER_TEST:
          testEvent = new AfterTestEvent(executionConsole);
          break;
        case BEFORE_SUITE:
          testEvent = new BeforeSuiteEvent(executionConsole);
          break;
        case AFTER_SUITE:
          testEvent = new AfterSuiteEvent(executionConsole);
          break;
        case UNKNOWN_EVENT:
          break;
      }
      if (testEvent != null) {
        testEvent.process(xml);
      }
    }
    catch (TestEventXmlView.XmlParserException e) {
      LOG.error("Gradle test events parser error", e);
    }
  }

  @Nullable
  private static String getEventMessage(@NotNull GradleTestsExecutionConsole executionConsole,
                                        @NotNull String text,
                                        @NotNull Key<?> processOutputType) {
    String eventMessage = null;
    final StringBuilder consoleBuffer = executionConsole.getBuffer();
    String trimmedText = text.trim();
    if (StringUtil.endsWith(trimmedText, LOG_EOL)) {
      consoleBuffer.append(StringUtil.trimEnd(trimmedText, LOG_EOL));
      return null;
    }
    else {
      if (consoleBuffer.length() == 0) {
        if (StringUtil.startsWith(trimmedText, LOG_START) && StringUtil.endsWith(trimmedText, LOG_END)) {
          eventMessage = text;
        }
        else {
          executionConsole.print(text, ConsoleViewContentType.getConsoleViewType(processOutputType));
          return null;
        }
      }
      else {
        consoleBuffer.append(text);
        if (trimmedText.isEmpty()) return null;
      }
    }

    if (eventMessage == null) {
      String bufferText = consoleBuffer.toString().trim();
      consoleBuffer.setLength(0);
      if (!StringUtil.startsWith(bufferText, LOG_START) || !StringUtil.endsWith(bufferText, LOG_END)) {
        executionConsole.print(bufferText, ConsoleViewContentType.getConsoleViewType(processOutputType));
        return null;
      }
      eventMessage = bufferText;
    }
    assert consoleBuffer.length() == 0;
    return eventMessage;
  }
}
