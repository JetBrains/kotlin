// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.build.*;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.FailureResult;
import com.intellij.build.events.impl.FinishBuildEventImpl;
import com.intellij.build.events.impl.StartBuildEventImpl;
import com.intellij.build.events.impl.SuccessResultImpl;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.console.DuplexConsoleView;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.FakeRerunAction;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.ArrayUtil;
import com.intellij.util.net.NetUtils;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.SerializationFilter;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;

import static com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.DEBUG_FORK_SOCKET_PARAM;
import static com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.DEBUG_SETUP_PREFIX;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUtil.convert;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUtil.getConsoleManagerFor;
import static com.intellij.openapi.util.text.StringUtil.nullize;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemRunConfiguration extends LocatableConfigurationBase implements SearchScopeProvidingRunProfile,
                                                                                          SMRunnerConsolePropertiesProvider {
  private static final ExtensionPointName<ExternalSystemRunConfigurationExtension> EP_NAME
    = ExtensionPointName.create("com.intellij.externalSystem.runConfigurationExtension");

  public static final Key<InputStream> RUN_INPUT_KEY = Key.create("RUN_INPUT_KEY");
  public static final Key<Class<? extends BuildProgressListener>> PROGRESS_LISTENER_KEY = Key.create("PROGRESS_LISTENER_KEY");

  private static final Logger LOG = Logger.getInstance(ExternalSystemRunConfiguration.class);
  private ExternalSystemTaskExecutionSettings mySettings = new ExternalSystemTaskExecutionSettings();
  private static final boolean DISABLE_FORK_DEBUGGER = Boolean.getBoolean("external.system.disable.fork.debugger");

  public ExternalSystemRunConfiguration(@NotNull ProjectSystemId externalSystemId,
                                        Project project,
                                        ConfigurationFactory factory,
                                        String name) {
    super(project, factory, name);
    mySettings.setExternalSystemIdString(externalSystemId.getId());
  }

  @Override
  public String suggestedName() {
    return AbstractExternalSystemTaskConfigurationType.generateName(getProject(), mySettings);
  }

  @Override
  public ExternalSystemRunConfiguration clone() {
    final Element element = new Element("toClone");
    try {
      writeExternal(element);
      RunConfiguration configuration = getFactory().createTemplateConfiguration(getProject());
      configuration.setName(getName());
      configuration.readExternal(element);
      return (ExternalSystemRunConfiguration)configuration;
    }
    catch (InvalidDataException | WriteExternalException e) {
      LOG.error(e);
      return null;
    }
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    Element e = element.getChild(ExternalSystemTaskExecutionSettings.TAG_NAME);
    if (e != null) {
      mySettings = XmlSerializer.deserialize(e, ExternalSystemTaskExecutionSettings.class);
    }
    EP_NAME.forEachExtensionSafe(extension -> extension.readExternal(this, element));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    element.addContent(XmlSerializer.serialize(mySettings, new SerializationFilter() {
      @Override
      public boolean accepts(@NotNull Accessor accessor, @NotNull Object bean) {
        // only these fields due to backward compatibility
        switch (accessor.getName()) {
          case "passParentEnvs":
            return !mySettings.isPassParentEnvs();
          case "env":
            return !mySettings.getEnv().isEmpty();
          default:
            return true;
        }
      }
    }));
    EP_NAME.forEachExtensionSafe(extension -> extension.writeExternal(this, element));
  }

  @NotNull
  public ExternalSystemTaskExecutionSettings getSettings() {
    return mySettings;
  }

  @NotNull
  @Override
  public SettingsEditor<ExternalSystemRunConfiguration> getConfigurationEditor() {
    SettingsEditorGroup<ExternalSystemRunConfiguration> group = new SettingsEditorGroup<>();
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"),
                    new ExternalSystemRunConfigurationEditor(getProject(), mySettings.getExternalSystemId()));
    EP_NAME.forEachExtensionSafe(extension -> extension.appendEditors(this, group));
    group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
    return group;
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
    // DebugExecutor ID  - com.intellij.execution.executors.DefaultDebugExecutor.EXECUTOR_ID
    String debugExecutorId = ToolWindowId.DEBUG;
    MyRunnableState runnableState = new MyRunnableState(mySettings, getProject(), debugExecutorId.equals(executor.getId()), this, env);
    copyUserDataTo(runnableState);
    return runnableState;
  }

  @Nullable
  @Override
  public GlobalSearchScope getSearchScope() {
    GlobalSearchScope scope = null;
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(mySettings.getExternalSystemId());
    if (manager != null) {
      scope = manager.getSearchScope(getProject(), mySettings);
    }
    if (scope == null) {
      VirtualFile file = VfsUtil.findFileByIoFile(new File(mySettings.getExternalProjectPath()), false);
      if (file != null) {
        Module module = DirectoryIndex.getInstance(getProject()).getInfoForFile(file).getModule();
        if (module != null) {
          scope = GlobalSearchScopes.executionScope(Collections.singleton(module));
        }
      }
    }
    return scope;
  }

  @Override
  public SMTRunnerConsoleProperties createTestConsoleProperties(Executor executor) {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(mySettings.getExternalSystemId());
    if (manager != null) {
      Object testConsoleProperties = manager.createTestConsoleProperties(getProject(), executor, this);
      return testConsoleProperties instanceof SMTRunnerConsoleProperties ? (SMTRunnerConsoleProperties)testConsoleProperties : null;
    }
    return null;
  }

  public static class MyRunnableState extends UserDataHolderBase implements RunProfileState {

    @NotNull private final ExternalSystemTaskExecutionSettings mySettings;
    @NotNull private final Project myProject;
    @NotNull private final ExternalSystemRunConfiguration myConfiguration;
    @NotNull private final ExecutionEnvironment myEnv;
    @Nullable private RunContentDescriptor myContentDescriptor;

    private final int myDebugPort;
    private ServerSocket myForkSocket = null;

    public MyRunnableState(@NotNull ExternalSystemTaskExecutionSettings settings,
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
          LOG.warn("Unexpected I/O exception occurred on attempt to find a free port to use for external system task debugging", e);
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
      if (myForkSocket == null && !DISABLE_FORK_DEBUGGER) {
        try {
          myForkSocket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
      return myForkSocket;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
      if (myProject.isDisposed()) return null;

      String jvmAgentSetup = getJvmAgentSetup();

      ApplicationManager.getApplication().assertIsDispatchThread();
      FileDocumentManager.getInstance().saveAllDocuments();

      final ExternalSystemExecuteTaskTask task = new ExternalSystemExecuteTaskTask(myProject, mySettings, jvmAgentSetup);
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
      Class<? extends BuildProgressListener> progressListenerClazz = task.getUserData(PROGRESS_LISTENER_KEY);
      final BuildProgressListener progressListener =
        progressListenerClazz != null ? ServiceManager.getService(myProject, progressListenerClazz)
                                      : createBuildView(task.getId(), executionName, task.getExternalProjectPath(), consoleView);

      EP_NAME.forEachExtensionSafe(extension -> extension.attachToProcess(myConfiguration, processHandler, myEnv.getRunnerSettings()));

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
                AnAction rerunTaskAction = new MyTaskRerunAction(progressListener, myEnv, myContentDescriptor);
                BuildViewSettingsProvider viewSettingsProvider =
                  consoleView instanceof BuildViewSettingsProvider ?
                  new BuildViewSettingsProviderAdapter((BuildViewSettingsProvider)consoleView) : null;
                progressListener.onEvent(id,
                  new StartBuildEventImpl(new DefaultBuildDescriptor(id, executionName, workingDir, eventTime), "running...")
                    .withProcessHandler(processHandler, view -> foldGreetingOrFarewell(consoleView, greeting, true))
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
              FailureResult failureResult =
                ExternalSystemUtil.createFailureResult(executionName + " failed", e, id.getProjectSystemId(), myProject);
              if (progressListener != null) {
                progressListener.onEvent(id, new FinishBuildEventImpl(id, null, System.currentTimeMillis(), "failed", failureResult));
              }
              processHandler.notifyProcessTerminated(1);
            }

            @Override
            public void onSuccess(@NotNull ExternalSystemTaskId id) {
              if (progressListener != null) {
                progressListener.onEvent(id, new FinishBuildEventImpl(
                  id, null, System.currentTimeMillis(), "successful", new SuccessResultImpl()));
              }
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
              foldGreetingOrFarewell(consoleView, farewell, false);
              processHandler.notifyProcessTerminated(0);
              eventDispatcher.close();
            }
          };
          task.execute(ArrayUtil.prepend(taskListener, ExternalSystemTaskNotificationListener.EP_NAME.getExtensions()));
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
    private String getJvmAgentSetup() throws ExecutionException {
      final SimpleJavaParameters extensionsJP = new SimpleJavaParameters();
      EP_NAME.forEachExtensionSafe(
        extension -> extension.updateVMParameters(myConfiguration, extensionsJP, myEnv.getRunnerSettings(), myEnv.getExecutor()));
      String jvmAgentSetup;
      if (myDebugPort > 0) {
        jvmAgentSetup = DEBUG_SETUP_PREFIX + myDebugPort;
        if (getForkSocket() != null) {
          jvmAgentSetup += (" " + DEBUG_FORK_SOCKET_PARAM + getForkSocket().getLocalPort());
        }
      }
      else {
        ParametersList parametersList = extensionsJP.getVMParametersList();
        final ParametersList data = myEnv.getUserData(ExternalSystemTaskExecutionSettings.JVM_AGENT_SETUP_KEY);
        if (data != null) {
          parametersList.addAll(data.getList());
        }
        for (String parameter : parametersList.getList()) {
          if (parameter.startsWith("-agentlib:")) continue;
          if (parameter.startsWith("-agentpath:")) continue;
          if (parameter.startsWith("-javaagent:")) continue;
          throw new ExecutionException(ExternalSystemBundle.message("run.invalid.jvm.agent.configuration", parameter));
        }
        jvmAgentSetup = parametersList.getParametersString();
      }
      return nullize(jvmAgentSetup);
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
  }

  private static void foldGreetingOrFarewell(@Nullable ExecutionConsole consoleView, String text, boolean isGreeting) {
    int limit = 100;
    if (text.length() < limit) {
      return;
    }
    final ConsoleViewImpl consoleViewImpl;
    if (consoleView instanceof ConsoleViewImpl) {
      consoleViewImpl = (ConsoleViewImpl)consoleView;
    }
    else if (consoleView instanceof DuplexConsoleView) {
      DuplexConsoleView duplexConsoleView = (DuplexConsoleView)consoleView;
      if (duplexConsoleView.getPrimaryConsoleView() instanceof ConsoleViewImpl) {
        consoleViewImpl = (ConsoleViewImpl)duplexConsoleView.getPrimaryConsoleView();
      }
      else if (duplexConsoleView.getSecondaryConsoleView() instanceof ConsoleViewImpl) {
        consoleViewImpl = (ConsoleViewImpl)duplexConsoleView.getSecondaryConsoleView();
      }
      else {
        consoleViewImpl = null;
      }
    }
    else {
      consoleViewImpl = null;
    }
    if (consoleViewImpl != null) {
      consoleViewImpl.performWhenNoDeferredOutput(() -> {
        if (!ApplicationManager.getApplication().isDispatchThread()) return;

        Document document = consoleViewImpl.getEditor().getDocument();
        int line = isGreeting ? 0 : document.getLineCount() - 2;
        if (CharArrayUtil.regionMatches(document.getCharsSequence(), document.getLineStartOffset(line), text)) {
          final FoldingModel foldingModel = consoleViewImpl.getEditor().getFoldingModel();
          foldingModel.runBatchFoldingOperation(() -> {
            FoldRegion region = foldingModel.addFoldRegion(document.getLineStartOffset(line),
                                                           document.getLineEndOffset(line) + 1,
                                                           StringUtil.trimLog(text, limit));
            if (region != null) {
              region.setExpanded(false);
            }
          });
        }
      });
    }
  }

  private static class MyTaskRerunAction extends FakeRerunAction {
    private final BuildProgressListener myProgressListener;
    private final RunContentDescriptor myContentDescriptor;
    private final ExecutionEnvironment myEnvironment;

    MyTaskRerunAction(BuildProgressListener progressListener,
                      ExecutionEnvironment environment,
                      RunContentDescriptor contentDescriptor) {
      myProgressListener = progressListener;
      myContentDescriptor = contentDescriptor;
      myEnvironment = environment;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      Presentation presentation = event.getPresentation();
      ExecutionEnvironment environment = getEnvironment(event);
      if (environment != null) {
        presentation.setText(ExecutionBundle.message("rerun.configuration.action.name",
                                                     StringUtil.escapeMnemonics(environment.getRunProfile().getName())));
        Icon icon = ExecutionManagerImpl.isProcessRunning(getDescriptor(event))
                    ? AllIcons.Actions.Restart
                    : myProgressListener instanceof BuildViewManager
                      ? AllIcons.Actions.Compile
                      : environment.getExecutor().getIcon();
        presentation.setIcon(icon);
        presentation.setEnabled(isEnabled(event));
        return;
      }

      presentation.setEnabled(false);
    }

    @Nullable
    @Override
    protected RunContentDescriptor getDescriptor(AnActionEvent event) {
      return myContentDescriptor != null ? myContentDescriptor : super.getDescriptor(event);
    }

    @Override
    protected ExecutionEnvironment getEnvironment(@NotNull AnActionEvent event) {
      return myEnvironment;
    }
  }
}
