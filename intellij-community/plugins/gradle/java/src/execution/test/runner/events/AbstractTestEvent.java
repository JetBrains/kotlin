/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.execution.testframework.JavaTestLocator;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.ui.SMTestRunnerResultsForm;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleConsoleProperties;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.intellij.util.io.URLUtil.SCHEME_SEPARATOR;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractTestEvent implements TestEvent {
  private final GradleTestsExecutionConsole myExecutionConsole;

  public AbstractTestEvent(GradleTestsExecutionConsole executionConsole) {
    this.myExecutionConsole = executionConsole;
  }

  public GradleTestsExecutionConsole getExecutionConsole() {
    return myExecutionConsole;
  }

  protected SMTestRunnerResultsForm getResultsViewer() {
    return myExecutionConsole.getResultsViewer();
  }

  protected Project getProject() {
    return myExecutionConsole.getProperties().getProject();
  }

  protected GradleConsoleProperties getProperties() {
    return (GradleConsoleProperties)getExecutionConsole().getProperties();
  }

  @NotNull
  protected String findLocationUrl(@Nullable String name, @NotNull String fqClassName) {
    return name == null
           ? JavaTestLocator.TEST_PROTOCOL + SCHEME_SEPARATOR + fqClassName
           : JavaTestLocator.TEST_PROTOCOL + SCHEME_SEPARATOR + StringUtil.getQualifiedName(fqClassName, StringUtil.trimEnd(name, "()"));
  }

  @Nullable
  protected SMTestProxy findTestProxy(final String proxyId) {
    return getExecutionConsole().getTestsMap().get(proxyId);
  }

  protected void registerTestProxy(final String proxyId, SMTestProxy testProxy) {
    myExecutionConsole.getTestsMap().put(proxyId, testProxy);
  }

  protected String decode(String s) {
    return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
  }
}
