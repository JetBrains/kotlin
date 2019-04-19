/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.JavaRerunFailedTestsAction;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.TestTreeView;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.history.actions.AbstractImportTestsAction;
import com.intellij.execution.testframework.sm.runner.ui.SMRootTestProxyFormatter;
import com.intellij.execution.testframework.sm.runner.ui.SMTestRunnerResultsForm;
import com.intellij.execution.testframework.sm.runner.ui.TestTreeRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.action.GradleRerunFailedTestsAction;
import org.jetbrains.plugins.gradle.execution.filters.ReRunTaskFilter;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class GradleTestsExecutionConsoleManager
  implements ExternalSystemExecutionConsoleManager<ExternalSystemRunConfiguration, GradleTestsExecutionConsole, ProcessHandler> {

  @NotNull
  @Override
  public ProjectSystemId getExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @Nullable
  @Override
  public GradleTestsExecutionConsole attachExecutionConsole(@NotNull Project project,
                                                            @NotNull ExternalSystemTask task,
                                                            @Nullable ExecutionEnvironment env,
                                                            @Nullable ProcessHandler processHandler) {
    if (env == null) return null;
    RunConfiguration configuration;
    SMTRunnerConsoleProperties consoleProperties = null;
    RunnerAndConfigurationSettings settings = env.getRunnerAndConfigurationSettings();
    if (settings == null) {
      RunProfile runProfile = env.getRunProfile();
      if (runProfile instanceof AbstractImportTestsAction.ImportRunProfile) {
        consoleProperties = ((AbstractImportTestsAction.ImportRunProfile)runProfile).getProperties();
        configuration = ((AbstractImportTestsAction.ImportRunProfile)runProfile).getInitialConfiguration();
      }
      else {
        return null;
      }
    } else {
      configuration = settings.getConfiguration();
    }
    if (!(configuration instanceof ExternalSystemRunConfiguration)) return null;
    ExternalSystemRunConfiguration externalSystemRunConfiguration = (ExternalSystemRunConfiguration)configuration;

    if(consoleProperties == null) {
      consoleProperties = new GradleConsoleProperties(externalSystemRunConfiguration, env.getExecutor());
    }
    String testFrameworkName = externalSystemRunConfiguration.getSettings().getExternalSystemId().getReadableName();
    String splitterPropertyName = SMTestRunnerConnectionUtil.getSplitterPropertyName(testFrameworkName);
    final GradleTestsExecutionConsole consoleView = new GradleTestsExecutionConsole(consoleProperties, splitterPropertyName);
    SMTestRunnerConnectionUtil.initConsoleView(consoleView, testFrameworkName);

    SMTestRunnerResultsForm resultsViewer = consoleView.getResultsViewer();
    final TestTreeView testTreeView = resultsViewer.getTreeView();
    if (testTreeView != null) {
      TestTreeRenderer originalRenderer = ObjectUtils.tryCast(testTreeView.getCellRenderer(), TestTreeRenderer.class);
      if (originalRenderer != null) {
        originalRenderer.setAdditionalRootFormatter(new SMRootTestProxyFormatter() {
          @Override
          public void format(@NotNull SMTestProxy.SMRootTestProxy testProxy, @NotNull TestTreeRenderer renderer) {
            if (!testProxy.isInProgress() && testProxy.isEmptySuite()) {
              renderer.clear();
              renderer.append(GradleBundle.message(
                "gradle.test.runner.ui.tests.tree.presentation.labels.no.tests.were.found"),
                              SimpleTextAttributes.REGULAR_ATTRIBUTES
              );
            }
          }
        });
      }
    }
    SMTestProxy.SMRootTestProxy testsRootNode = resultsViewer.getTestsRootNode();
    testsRootNode.setSuiteStarted();
    if (processHandler != null) {
      processHandler.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          if (testsRootNode.isInProgress()) {
            ApplicationManager.getApplication().invokeLater(() -> {
              if (event.getExitCode() == 1) {
                testsRootNode.setTestFailed("", null, false);
              }
              else {
                testsRootNode.setFinished();
              }
              resultsViewer.onBeforeTestingFinished(testsRootNode);
              resultsViewer.onTestingFinished(testsRootNode);
            });
          }
        }
      });
    }

    if (task instanceof ExternalSystemExecuteTaskTask) {
      final ExternalSystemExecuteTaskTask executeTask = (ExternalSystemExecuteTaskTask)task;
      if (executeTask.getArguments() == null || !StringUtil.contains(executeTask.getArguments(), GradleConstants.TESTS_ARG_NAME)) {
        executeTask.appendArguments("--tests *");
      }
      consoleView.addMessageFilter(new ReRunTaskFilter((ExternalSystemExecuteTaskTask)task, env));
    }

    return consoleView;
  }

  @Override
  public void onOutput(@NotNull GradleTestsExecutionConsole executionConsole,
                       @NotNull ProcessHandler processHandler,
                       @NotNull String text,
                       @NotNull Key processOutputType) {
    GradleTestsExecutionConsoleOutputProcessor.onOutput(executionConsole, text, processOutputType);
  }

  @Override
  public boolean isApplicableFor(@NotNull ExternalSystemTask task) {
    if (task instanceof ExternalSystemExecuteTaskTask) {
      final ExternalSystemExecuteTaskTask taskTask = (ExternalSystemExecuteTaskTask)task;
      if (!StringUtil.equals(taskTask.getExternalSystemId().getId(), GradleConstants.SYSTEM_ID.getId())) return false;

      final String arguments = taskTask.getArguments();
      if (arguments != null && StringUtil.contains(arguments, GradleConstants.TESTS_ARG_NAME)) return true;

      return ContainerUtil.find(taskTask.getTasksToExecute(), taskToExecute -> {
        String projectPath = taskTask.getExternalProjectPath();
        File file = new File(projectPath);
        if (file.isFile()) {
          projectPath = StringUtil.trimEnd(projectPath, "/" + file.getName());
        }
        final ExternalProjectInfo externalProjectInfo =
          ExternalSystemUtil.getExternalProjectInfo(taskTask.getIdeProject(), getExternalSystemId(), projectPath);
        if (externalProjectInfo == null) return false;

        final DataNode<TaskData> taskDataNode = GradleProjectResolverUtil.findTask(
          externalProjectInfo.getExternalProjectStructure(), projectPath, taskToExecute);
        return taskDataNode != null &&
               (taskDataNode.getData().isTest() ||
                "check".equals(taskDataNode.getData().getName()) && "verification".equals(taskDataNode.getData().getGroup()));
      }) != null;
    }
    return false;
  }

  @Override
  public AnAction[] getRestartActions(@NotNull final GradleTestsExecutionConsole consoleView) {
    JavaRerunFailedTestsAction rerunFailedTestsAction =
      new GradleRerunFailedTestsAction(consoleView);
    rerunFailedTestsAction.setModelProvider(() -> consoleView.getResultsViewer());
    return new AnAction[]{rerunFailedTestsAction};
  }
}
