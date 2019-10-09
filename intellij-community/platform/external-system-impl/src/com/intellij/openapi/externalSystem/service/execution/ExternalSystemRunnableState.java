// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.build.*;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FailureResult;
import com.intellij.build.events.impl.FailureResultImpl;
import com.intellij.build.events.impl.FinishBuildEventImpl;
import com.intellij.build.events.impl.StartBuildEventImpl;
import com.intellij.build.events.impl.SuccessResultImpl;
import com.intellij.execution.*;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent;
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.net.NetUtils;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.DEBUG_FORK_SOCKET_PARAM;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUtil.convert;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUtil.getConsoleManagerFor;
import static com.intellij.openapi.util.text.StringUtil.nullize;

public class ExternalSystemRunnableState extends UserDataHolderBase implements RunProfileState {

  @NotNull private final ExternalSystemTaskExecutionSettings mySettings;
  @NotNull private final Project myProject;
  @NotNull private final ExternalSystemRunConfiguration myConfiguration;
  @NotNull private final ExecutionEnvironment myEnv;
  @Nullable private RunContentDescriptor myContentDescriptor;

  private final int myDebugPort;
  private ServerSocket myForkSocket = null;

  public ExternalSystemRunnableState(@NotNull ExternalSystemTaskExecutionSettings settings,
                                     @NotNull Project project,
                                     boolean debug,
                                     @NotNull ExternalSystemRunConfiguration configuration,
                                     @NotNull ExecutionEnvironment env) {
    mySettings = settings;
    myProject = project;
    myConfiguration = configuration;
    myEnv = env;
    int port;
    if (debug) {
      try {
        port = NetUtils.findAvailableSocketPort();
      }
      catch (IOException e) {
        ExternalSystemRunConfiguration.LOG
          .warn("Unexpected I/O exception occurred on attempt to find a free port to use for external system task debugging", e);
        port = 0;
      }
    }
    else {
      port = 0;
    }
    myDebugPort = port;
  }

  public int getDebugPort() {
    return myDebugPort;
  }

  @Nullable
  public ServerSocket getForkSocket() {
    if (myForkSocket == null && !ExternalSystemRunConfiguration.DISABLE_FORK_DEBUGGER) {
      try {
        myForkSocket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
      }
      catch (IOException e) {
        ExternalSystemRunConfiguration.LOG.error(e);
      }
    }
    return myForkSocket;
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
    if (myProject.isDisposed()) return null;

    String jvmParametersSetup = getJvmParametersSetup();

    ApplicationManager.getApplication().assertIsDispatchThread();
    FileDocumentManager.getInstance().saveAllDocuments();

    final ExternalSystemExecuteTaskTask task = new ExternalSystemExecuteTaskTask(myProject, mySettings, jvmParametersSetup);
    copyUserDataTo(task);

    final String executionName = StringUtil.isNotEmpty(mySettings.getExecutionName())
                                 ? mySettings.getExecutionName()
                                 : StringUtil.isNotEmpty(myConfiguration.getName())
                                   ? myConfiguration.getName() : AbstractExternalSystemTaskConfigurationType.generateName(
                                   myProject, mySettings.getExternalSystemId(), mySettings.getExternalProjectPath(),
                                   mySettings.getTaskNames(), mySettings.getExecutionName(), ": ", "");

    final ExternalSystemProcessHandler processHandler = new ExternalSystemProcessHandler(task, executionName);
    final ExternalSystemExecutionConsoleManager<ExternalSystemRunConfiguration, ExecutionConsole, ProcessHandler>
      consoleManager = getConsoleManagerFor(task);

    final ExecutionConsole consoleView =
      consoleManager.attachExecutionConsole(myProject, task, myEnv, processHandler);
    AnAction[] restartActions;
    if (consoleView == null) {
      restartActions = AnAction.EMPTY_ARRAY;
      Disposer.register(myProject, processHandler);
    }
    else {
      Disposer.register(myProject, consoleView);
      Disposer.register(consoleView, processHandler);
      restartActions = consoleManager.getRestartActions(consoleView);
    }
    Class<? extends BuildProgressListener> progressListenerClazz = task.getUserData(ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY);
    final BuildProgressListener progressListener =
      progressListenerClazz != null ? ServiceManager.getService(myProject, progressListenerClazz)
                                    : createBuildView(task.getId(), executionName, task.getExternalProjectPath(), consoleView);

    ExternalSystemRunConfiguration.EP_NAME
      .forEachExtensionSafe(extension -> extension.attachToProcess(myConfiguration, processHandler, myEnv.getRunnerSettings()));

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      final String startDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
      final String greeting;
      final String settingsDescription = StringUtil.isEmpty(mySettings.toString()) ? "" : String.format(" '%s'", mySettings.toString());
      if (mySettings.getTaskNames().size() > 1) {
        greeting = ExternalSystemBundle.message("run.text.starting.multiple.task", startDateTime, settingsDescription) + "\n";
      }
      else {
        greeting = ExternalSystemBundle.message("run.text.starting.single.task", startDateTime, settingsDescription) + "\n";
      }
      processHandler.notifyTextAvailable(greeting + "\n", ProcessOutputTypes.SYSTEM);
      try (BuildEventDispatcher eventDispatcher = new ExternalSystemEventDispatcher(task.getId(), progressListener, false)) {
        ExternalSystemTaskNotificationListenerAdapter taskListener = new ExternalSystemTaskNotificationListenerAdapter() {
          @Override
          public void onStart(@NotNull ExternalSystemTaskId id, String workingDir) {
            if (progressListener != null) {
              long eventTime = System.currentTimeMillis();
              AnAction rerunTaskAction = new ExternalSystemRunConfiguration.MyTaskRerunAction(progressListener, myEnv, myContentDescriptor);
              BuildViewSettingsProvider viewSettingsProvider =
                consoleView instanceof BuildViewSettingsProvider ?
                new BuildViewSettingsProviderAdapter((BuildViewSettingsProvider)consoleView) : null;
              progressListener.onEvent(id,
                                       new StartBuildEventImpl(new DefaultBuildDescriptor(id, executionName, workingDir, eventTime),
                                                               "running...")
                                         .withProcessHandler(processHandler, view -> ExternalSystemRunConfiguration
                                           .foldGreetingOrFarewell(consoleView, greeting, true))
                                         .withContentDescriptorSupplier(() -> myContentDescriptor)
                                         .withRestartAction(rerunTaskAction)
                                         .withRestartActions(restartActions)
                                         .withExecutionEnvironment(myEnv)
                                         .withBuildViewSettingsProvider(viewSettingsProvider)
              );
            }
          }

          @Override
          public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
            if (consoleView != null) {
              consoleManager.onOutput(consoleView, processHandler, text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
            }
            else {
              processHandler.notifyTextAvailable(text, stdOut ? ProcessOutputTypes.STDOUT : ProcessOutputTypes.STDERR);
            }
            eventDispatcher.setStdOut(stdOut);
            eventDispatcher.append(text);
          }

          @Override
          public void onFailure(@NotNull ExternalSystemTaskId id, @NotNull Exception e) {
            DataProvider dataProvider = BuildConsoleUtils.getDataProvider(id, progressListener);
            FailureResult failureResult =
              ExternalSystemUtil.createFailureResult(executionName + " failed", e, id.getProjectSystemId(), myProject, dataProvider);
            eventDispatcher.onEvent(id, new FinishBuildEventImpl(id, null, System.currentTimeMillis(), "failed", failureResult));
            processHandler.notifyProcessTerminated(1);
          }

          @Override
          public void onSuccess(@NotNull ExternalSystemTaskId id) {
            eventDispatcher.onEvent(id, new FinishBuildEventImpl(
              id, null, System.currentTimeMillis(), "successful", new SuccessResultImpl()));
          }

          @Override
          public void onStatusChange(@NotNull ExternalSystemTaskNotificationEvent event) {
            if (event instanceof ExternalSystemBuildEvent) {
              eventDispatcher.onEvent(event.getId(), ((ExternalSystemBuildEvent)event).getBuildEvent());
            }
            else if (event instanceof ExternalSystemTaskExecutionEvent) {
              BuildEvent buildEvent = convert(((ExternalSystemTaskExecutionEvent)event));
              eventDispatcher.onEvent(event.getId(), buildEvent);
            }
          }

          @Override
          public void onEnd(@NotNull ExternalSystemTaskId id) {
            final String endDateTime = DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis());
            final String farewell;
            if (mySettings.getTaskNames().size() > 1) {
              farewell = ExternalSystemBundle.message("run.text.ended.multiple.task", endDateTime, settingsDescription);
            }
            else {
              farewell = ExternalSystemBundle.message("run.text.ended.single.task", endDateTime, settingsDescription);
            }
            processHandler.notifyTextAvailable(farewell + "\n", ProcessOutputTypes.SYSTEM);
            ExternalSystemRunConfiguration.foldGreetingOrFarewell(consoleView, farewell, false);
            processHandler.notifyProcessTerminated(0);
          }
        };
        task.execute(ArrayUtil.prepend(taskListener, ExternalSystemTaskNotificationListener.EP_NAME.getExtensions()));
        Throwable taskError = task.getError();
        if (taskError != null && !(taskError instanceof Exception)) {
          FinishBuildEventImpl failureEvent = new FinishBuildEventImpl(task.getId(), null, System.currentTimeMillis(), "failed",
                                                                       new FailureResultImpl(taskError));
          eventDispatcher.onEvent(task.getId(), failureEvent);
        }
      }
    });
    ExecutionConsole executionConsole = progressListener instanceof ExecutionConsole ? (ExecutionConsole)progressListener : consoleView;
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    if (executionConsole instanceof BuildView) {
      actionGroup.addAll(((BuildView)executionConsole).getSwitchActions());
      actionGroup.add(BuildTreeFilters.createFilteringActionsGroup((BuildView)executionConsole));
    }
    DefaultExecutionResult executionResult = new DefaultExecutionResult(executionConsole, processHandler, actionGroup.getChildren(null));
    executionResult.setRestartActions(restartActions);
    return executionResult;
  }

  @Nullable
  private String getJvmParametersSetup() throws ExecutionException {
    final SimpleJavaParameters extensionsJP = new SimpleJavaParameters();
    ExternalSystemRunConfiguration.EP_NAME.forEachExtensionSafe(
      extension -> extension.updateVMParameters(myConfiguration, extensionsJP, myEnv.getRunnerSettings(), myEnv.getExecutor()));
    String jvmParametersSetup;
    if (myDebugPort > 0) {
      jvmParametersSetup = ForkedDebuggerHelper.JVM_DEBUG_SETUP_PREFIX + myDebugPort;
      if (getForkSocket() != null) {
        jvmParametersSetup += (" " + DEBUG_FORK_SOCKET_PARAM + getForkSocket().getLocalPort());
      }
    }
    else {
      final ParametersList allVMParameters = new ParametersList();
      final ParametersList data = myEnv.getUserData(ExternalSystemTaskExecutionSettings.JVM_AGENT_SETUP_KEY);
      if (data != null) {
        for (String parameter : data.getList()) {
          if (parameter.startsWith("-agentlib:")) continue;
          if (parameter.startsWith("-agentpath:")) continue;
          if (parameter.startsWith("-javaagent:")) continue;
          throw new ExecutionException(ExternalSystemBundle.message("run.invalid.jvm.agent.configuration", parameter));
        }
        allVMParameters.addAll(data.getParameters());
      }
      allVMParameters.addAll(extensionsJP.getVMParametersList().getParameters());
      jvmParametersSetup = allVMParameters.getParametersString();
    }
    return nullize(jvmParametersSetup);
  }

  private BuildProgressListener createBuildView(ExternalSystemTaskId id,
                                                String executionName,
                                                String workingDir,
                                                ExecutionConsole executionConsole) {
    BuildDescriptor buildDescriptor = new DefaultBuildDescriptor(id, executionName, workingDir, System.currentTimeMillis());
    return new BuildView(myProject, executionConsole, buildDescriptor, "build.toolwindow.run.selection.state",
                         new ViewManager() {
                           @Override
                           public boolean isConsoleEnabledByDefault() {
                             return true;
                           }

                           @Override
                           public boolean isBuildContentView() {
                             return false;
                           }
                         });
  }

  public void setContentDescriptor(@Nullable RunContentDescriptor contentDescriptor) {
    myContentDescriptor = contentDescriptor;
    if (contentDescriptor != null) {
      contentDescriptor.setExecutionId(myEnv.getExecutionId());
      RunnerAndConfigurationSettings settings = myEnv.getRunnerAndConfigurationSettings();
      if (settings != null) {
        contentDescriptor.setActivateToolWindowWhenAdded(settings.isActivateToolWindowBeforeRun());
      }
    }
  }

  @Nullable
  public XDebugProcess startDebugProcess(@NotNull XDebugSession session,
                                         @NotNull ExecutionEnvironment env) {
    return null;
  }
}
