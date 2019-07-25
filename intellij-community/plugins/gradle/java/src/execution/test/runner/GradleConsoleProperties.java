// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.Executor;
import com.intellij.execution.Location;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.testframework.JavaAwareTestConsoleProperties;
import com.intellij.execution.testframework.JavaTestLocator;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.pom.Navigatable;
import com.intellij.util.config.BooleanProperty;
import com.intellij.util.config.DumbAwareToggleBooleanProperty;
import com.intellij.util.config.ToggleBooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleBundle;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class GradleConsoleProperties extends SMTRunnerConsoleProperties {
  public static final BooleanProperty SHOW_INTERNAL_TEST_NODES = new BooleanProperty("showInternalTestNodes", false);
  public static final SMTestLocator GRADLE_TEST_LOCATOR = JavaTestLocator.INSTANCE;

  @Nullable private File gradleTestReport;

  public GradleConsoleProperties(final ExternalSystemRunConfiguration configuration, Executor executor) {
    this(configuration, configuration.getSettings().getExternalSystemId().getReadableName(), executor);
  }

  public GradleConsoleProperties(@NotNull RunConfiguration config, @NotNull String testFrameworkName, @NotNull Executor executor) {
    super(config, testFrameworkName, executor);
  }

  public void setGradleTestReport(@Nullable File gradleTestReport) {
    this.gradleTestReport = gradleTestReport;
  }

  @Nullable
  public File getGradleTestReport() {
    return gradleTestReport;
  }

  @Override
  public int getSelectionMode() {
    return TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
  }

  @Override
  public void appendAdditionalActions(DefaultActionGroup actionGroup, JComponent parent, TestConsoleProperties target) {
    super.appendAdditionalActions(actionGroup, parent, target);
    actionGroup.add(Separator.getInstance());
    actionGroup.add(createShowInternalNodesAction(target));
  }

  @Nullable
  @Override
  public Navigatable getErrorNavigatable(@NotNull Location<?> location, @NotNull String stacktrace) {
    return JavaAwareTestConsoleProperties.getStackTraceErrorNavigatable(location, stacktrace);
  }

  @Nullable
  @Override
  public SMTestLocator getTestLocator() {
    return GRADLE_TEST_LOCATOR;
  }

  @NotNull
  private ToggleBooleanProperty createShowInternalNodesAction(TestConsoleProperties target) {
    String text = GradleBundle.message("gradle.test.show.internal.nodes.action.name");
    setIfUndefined(SHOW_INTERNAL_TEST_NODES, false);
    String desc = GradleBundle.message("gradle.test.show.internal.nodes.action.text");
    return new DumbAwareToggleBooleanProperty(text, desc, null, target, SHOW_INTERNAL_TEST_NODES);
  }
}
