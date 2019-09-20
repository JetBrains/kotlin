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
package org.jetbrains.plugins.gradle.action;

import com.intellij.execution.Executor;
import com.intellij.execution.actions.JavaRerunFailedTestsAction;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleSMTestProxy;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.util.containers.ContainerUtil.filterIsInstance;
import static org.jetbrains.plugins.gradle.execution.test.runner.GradleTestRunConfigurationProducer.findAllTestsTaskToRun;
import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.*;
import static org.jetbrains.plugins.gradle.util.GradleRerunFailedTasksActionUtilsKt.containsSubSequenceInSequence;
import static org.jetbrains.plugins.gradle.util.GradleRerunFailedTasksActionUtilsKt.containsTasksInScriptParameters;

/**
 * @author Vladislav.Soroka
 */
public class GradleRerunFailedTestsAction extends JavaRerunFailedTestsAction {
  public GradleRerunFailedTestsAction(GradleTestsExecutionConsole consoleView) {
    super(consoleView.getConsole(), consoleView.getProperties());
  }

  @Nullable
  @Override
  protected MyRunProfile getRunProfile(@NotNull ExecutionEnvironment environment) {
    ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration)myConsoleProperties.getConfiguration();
    final List<AbstractTestProxy> failedTests = getFailedTests(configuration.getProject());
    return new MyRunProfile(configuration) {
      @Nullable
      @Override
      public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        ExternalSystemRunConfiguration runProfile = ((ExternalSystemRunConfiguration)getPeer()).clone();
        Project project = runProfile.getProject();
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        final GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        ExternalSystemTaskExecutionSettings settings = runProfile.getSettings().clone();
        List<GradleSMTestProxy> tests = filterIsInstance(failedTests, GradleSMTestProxy.class);
        Function1<GradleSMTestProxy, VirtualFile> findTestSource = test -> {
          String className = test.getClassName();
          if (className == null) return null;
          return getSourceFile(javaPsiFacade.findClass(className, projectScope));
        };
        Function1<GradleSMTestProxy, String> createFilter = (test) -> {
          String testName = test.getName();
          String className = test.getClassName();
          return TestMethodGradleConfigurationProducer.createTestFilter(className, testName)  ;
        };
        Function1<VirtualFile, List<List<String>>> getTestsTaskToRun = source -> {
          List<? extends List<String>> foundTasksToRun = findAllTestsTaskToRun(source, project);
          List<List<String>> tasksToRun = new ArrayList<>();
          boolean isSpecificTask = false;
          for (List<String> tasks : foundTasksToRun) {
            List<String> escapedTasks = ContainerUtil.map(tasks, it -> escapeIfNeeded(it));
            if (containsSubSequenceInSequence(runProfile.getSettings().getTaskNames(), escapedTasks) ||
                containsTasksInScriptParameters(runProfile.getSettings().getScriptParameters(), escapedTasks)) {
              ContainerUtil.addAllNotNull(tasksToRun, tasks);
              isSpecificTask = true;
            }
          }
          if (!isSpecificTask && !foundTasksToRun.isEmpty()) {
            ContainerUtil.addAllNotNull(tasksToRun, foundTasksToRun.iterator().next());
          }
          return tasksToRun;
        };
        String projectPath = settings.getExternalProjectPath();
        if (applyTestConfiguration(settings, projectPath, tests, findTestSource, createFilter, getTestsTaskToRun)) {
          runProfile.getSettings().setFrom(settings);
        }
        return runProfile.getState(executor, environment);
      }
    };
  }
}
