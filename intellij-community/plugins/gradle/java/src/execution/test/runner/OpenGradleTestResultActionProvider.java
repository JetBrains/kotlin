// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.TestFrameworkRunningModel;
import com.intellij.execution.testframework.ToggleModelAction;
import com.intellij.execution.testframework.ToggleModelActionProvider;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.util.config.AbstractProperty;
import com.intellij.util.config.BooleanProperty;
import icons.GradleIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;


/**
 * @author Vladislav.Soroka
 */
public class OpenGradleTestResultActionProvider implements ToggleModelActionProvider {
  public static final BooleanProperty OPEN_GRADLE_REPORT = new BooleanProperty("openGradleReport", false);

  @Override
  public ToggleModelAction createToggleModelAction(TestConsoleProperties properties) {
    return new MyToggleModelAction(properties);
  }

  private static class MyToggleModelAction extends ToggleModelAction {
    @Nullable
    private ProjectSystemId mySystemId;

    MyToggleModelAction(TestConsoleProperties properties) {
      super(GradleBundle.message("gradle.test.runner.ui.tests.actions.open.gradle.report.text"),
            GradleBundle.message("gradle.test.runner.ui.tests.actions.open.gradle.report.desc"),
                                 GradleIcons.GradleNavigate, properties, OPEN_GRADLE_REPORT);
    }

    @Override
    public void setModel(TestFrameworkRunningModel model) {
      final RunProfile runConfiguration = model.getProperties().getConfiguration();
      if(runConfiguration instanceof ExternalSystemRunConfiguration) {
        mySystemId = ((ExternalSystemRunConfiguration)runConfiguration).getSettings().getExternalSystemId();
      }
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      final String reportFilePath = getReportFilePath();
      if (reportFilePath != null) {
        BrowserUtil.browse(reportFilePath);
      }
    }

    @Override
    protected boolean isEnabled() {
      final String reportFilePath = getReportFilePath();
      return reportFilePath != null;
    }

    @Override
    protected boolean isVisible() {
      return GradleConstants.SYSTEM_ID.equals(mySystemId);
    }

    @Nullable
    private String getReportFilePath() {
      final AbstractProperty.AbstractPropertyContainer properties = getProperties();
      if (properties instanceof GradleConsoleProperties) {
        GradleConsoleProperties gradleConsoleProperties = (GradleConsoleProperties)properties;
        final File testReport = gradleConsoleProperties.getGradleTestReport();
        if (testReport != null && testReport.isFile()) return testReport.getPath();
      }
      return null;
    }
  }
}
