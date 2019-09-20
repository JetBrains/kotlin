// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.startup;

import com.intellij.execution.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Irina.Chernushina on 8/19/2015.
 */
public class ProjectStartupRunner implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@NotNull final Project project) {
    final ProjectStartupTaskManager projectStartupTaskManager = ProjectStartupTaskManager.getInstance(project);
    if (projectStartupTaskManager.isEmpty()) return;

    project.getMessageBus().connect().subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
      @Override
      public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        projectStartupTaskManager.delete(settings.getUniqueID());
      }

      @Override
      public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings, String existingId) {
        if (existingId != null) {
          projectStartupTaskManager.rename(existingId, settings);
        }
        projectStartupTaskManager.checkOnChange(settings);
      }

      @Override
      public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
        projectStartupTaskManager.checkOnChange(settings);
      }
    });

    projectStartupTaskManager.waitForExecutionReady(() -> runActivities(project));
  }

  private static void runActivities(final Project project) {
    final ProjectStartupTaskManager projectStartupTaskManager = ProjectStartupTaskManager.getInstance(project);
    final List<RunnerAndConfigurationSettings> configurations =
      new ArrayList<>(projectStartupTaskManager.getLocalConfigurations());
    configurations.addAll(projectStartupTaskManager.getSharedConfigurations());

    ApplicationManager.getApplication().invokeLater(() -> {
      long pause = 0;
      final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
      final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
      for (final RunnerAndConfigurationSettings configuration : configurations) {
        if (! canBeRun(configuration)) {
          showNotification(project, "Run Configuration '" + configuration.getName() + "' can not be started with 'Run' action.", MessageType.ERROR);
          return;
        }

        try {
          alarm.addRequest(new MyExecutor(executor, configuration, alarm), pause);
        }
        catch (ExecutionException e) {
          showNotification(project, e.getMessage(), MessageType.ERROR);
        }
        pause = MyExecutor.PAUSE;
      }
    }, project.getDisposed());
  }

  private static void showNotification(Project project, String text, MessageType type) {
    ProjectStartupTaskManager.NOTIFICATION_GROUP.createNotification(ProjectStartupTaskManager.PREFIX + " " + text, type).notify(project);
  }

  private static class MyExecutor implements Runnable {
    public static final int ATTEMPTS = 10;
    private final ExecutionEnvironment myEnvironment;
    @NotNull private final Alarm myAlarm;
    private final Project myProject;
    private int myCnt = ATTEMPTS;
    private final static long PAUSE = 300;
    private final String myName;

    MyExecutor(@NotNull final Executor executor, @NotNull final RunnerAndConfigurationSettings configuration,
                      @NotNull Alarm alarm) throws ExecutionException {
      myName = configuration.getName();
      myProject = configuration.getConfiguration().getProject();
      myAlarm = alarm;
      myEnvironment = ExecutionEnvironmentBuilder.create(executor, configuration).contentToReuse(null).dataContext(null)
        .activeTarget().build();
    }

    @Override
    public void run() {
      if (ExecutorRegistry.getInstance().isStarting(myEnvironment)) {
        if (myCnt <= 0) {
          showNotification(myProject, "'" + myName + "' not started after " + ATTEMPTS + " attempts.", MessageType.ERROR);
          return;
        }
        --myCnt;
        myAlarm.addRequest(this, PAUSE);
      }
      // reporting that the task successfully started would require changing the interface of execution subsystem, not it reports errors by itself
      ProjectStartupTaskManager.NOTIFICATION_GROUP
        .createNotification(ProjectStartupTaskManager.PREFIX + " starting '" + myName + "'", MessageType.INFO).notify(myProject);
      ProgramRunnerUtil.executeConfiguration(myEnvironment, true, true);
      // same thread always
      if (myAlarm.isEmpty()) Disposer.dispose(myAlarm);
    }
  }

  public static boolean canBeRun(@NotNull RunnerAndConfigurationSettings configuration) {
    return ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration.getConfiguration()) != null;
  }
}
