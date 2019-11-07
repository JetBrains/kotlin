// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tools;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.ide.macro.Macro;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.SchemeElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Tool implements SchemeElement {
  private static final Logger LOG = Logger.getInstance(Tool.class);

  @NonNls public static final String ACTION_ID_PREFIX = "Tool_";

  public static final String DEFAULT_GROUP_NAME = "External Tools";
  private String myName;
  private String myDescription;
  @NotNull private String myGroup = DEFAULT_GROUP_NAME;

  // These 4 fields and everything related are effectively not used anymore, see IDEA-190856.
  // Let's keep them for a while for compatibility in case we have to reconsider.
  private boolean myShownInMainMenu;
  private boolean myShownInEditor;
  private boolean myShownInProjectViews;
  private boolean myShownInSearchResultsPopup;

  private boolean myEnabled;

  private boolean myUseConsole;
  private boolean myShowConsoleOnStdOut;
  private boolean myShowConsoleOnStdErr;
  private boolean mySynchronizeAfterExecution;

  private String myWorkingDirectory;
  private String myProgram;
  private String myParameters;

  private ArrayList<FilterInfo> myOutputFilters = new ArrayList<>();

  public Tool() {
  }

  public String getName() {
    return myName;
  }

  public String getDescription() {
    return myDescription;
  }

  @NotNull
  public String getGroup() {
    return myGroup;
  }

  public boolean isShownInMainMenu() {
    return myShownInMainMenu;
  }

  public boolean isShownInEditor() {
    return myShownInEditor;
  }

  public boolean isShownInProjectViews() {
    return myShownInProjectViews;
  }

  public boolean isShownInSearchResultsPopup() {
    return myShownInSearchResultsPopup;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  public boolean isUseConsole() {
    return myUseConsole;
  }

  public boolean isShowConsoleOnStdOut() {
    return myShowConsoleOnStdOut;
  }

  public boolean isShowConsoleOnStdErr() {
    return myShowConsoleOnStdErr;
  }

  public boolean synchronizeAfterExecution() {
    return mySynchronizeAfterExecution;
  }

  void setName(String name) {
    myName = name;
  }

  void setDescription(String description) {
    myDescription = description;
  }

  void setGroup(@NotNull String group) {
    myGroup = StringUtil.isEmpty(group)?DEFAULT_GROUP_NAME:group;
  }

  void setShownInMainMenu(boolean shownInMainMenu) {
    myShownInMainMenu = shownInMainMenu;
  }

  void setShownInEditor(boolean shownInEditor) {
    myShownInEditor = shownInEditor;
  }

  void setShownInProjectViews(boolean shownInProjectViews) {
    myShownInProjectViews = shownInProjectViews;
  }

  public void setShownInSearchResultsPopup(boolean shownInSearchResultsPopup) {
    myShownInSearchResultsPopup = shownInSearchResultsPopup;
  }

  void setUseConsole(boolean useConsole) {
    myUseConsole = useConsole;
  }

  void setShowConsoleOnStdOut(boolean showConsole) {
    myShowConsoleOnStdOut = showConsole;
  }

  void setShowConsoleOnStdErr(boolean showConsole) {
    myShowConsoleOnStdErr = showConsole;
  }

  public void setFilesSynchronizedAfterRun(boolean synchronizeAfterRun) {
    mySynchronizeAfterExecution = synchronizeAfterRun;
  }

  public String getWorkingDirectory() {
    return myWorkingDirectory;
  }

  public void setWorkingDirectory(String workingDirectory) {
    myWorkingDirectory = workingDirectory;
  }

  public String getProgram() {
    return myProgram;
  }

  public void setProgram(String program) {
    myProgram = program;
  }

  public String getParameters() {
    return myParameters;
  }

  public void setParameters(String parameters) {
    myParameters = parameters;
  }

  public void addOutputFilter(FilterInfo filter) {
    myOutputFilters.add(filter);
  }

  public void setOutputFilters(FilterInfo[] filters) {
    myOutputFilters = new ArrayList<>();
    if (filters != null) {
      Collections.addAll(myOutputFilters, filters);
    }
  }

  public FilterInfo[] getOutputFilters() {
    return myOutputFilters.toArray(new FilterInfo[0]);
  }

  public void copyFrom(Tool source) {
    myName = source.getName();
    myDescription = source.getDescription();
    myGroup = source.getGroup();
    myShownInMainMenu = source.isShownInMainMenu();
    myShownInEditor = source.isShownInEditor();
    myShownInProjectViews = source.isShownInProjectViews();
    myShownInSearchResultsPopup = source.isShownInSearchResultsPopup();
    myEnabled = source.isEnabled();
    myUseConsole = source.isUseConsole();
    myShowConsoleOnStdOut = source.isShowConsoleOnStdOut();
    myShowConsoleOnStdErr = source.isShowConsoleOnStdErr();
    mySynchronizeAfterExecution = source.synchronizeAfterExecution();
    myWorkingDirectory = source.getWorkingDirectory();
    myProgram = source.getProgram();
    myParameters = source.getParameters();
    myOutputFilters = new ArrayList<>(Arrays.asList(source.getOutputFilters()));
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof Tool)) {
      return false;
    }

    Tool source = (Tool)obj;
    return
      Comparing.equal(myName, source.myName) &&
      Comparing.equal(myDescription, source.myDescription) &&
      Comparing.equal(myGroup, source.myGroup) &&
      myShownInMainMenu == source.myShownInMainMenu &&
      myShownInEditor == source.myShownInEditor &&
      myShownInProjectViews == source.myShownInProjectViews &&
      myShownInSearchResultsPopup == source.myShownInSearchResultsPopup &&
      myEnabled == source.myEnabled &&
      myUseConsole == source.myUseConsole &&
      myShowConsoleOnStdOut == source.myShowConsoleOnStdOut &&
      myShowConsoleOnStdErr == source.myShowConsoleOnStdErr &&
      mySynchronizeAfterExecution == source.mySynchronizeAfterExecution &&
      Comparing.equal(myWorkingDirectory, source.myWorkingDirectory) &&
      Comparing.equal(myProgram, source.myProgram) &&
      Comparing.equal(myParameters, source.myParameters) &&
      Comparing.equal(myOutputFilters, source.myOutputFilters);
  }

  @NotNull
  public String getActionId() {
    StringBuilder name = new StringBuilder(getActionIdPrefix());
    name.append(myGroup);
    name.append('_');
    if (myName != null) {
      name.append(myName);
    }
    return name.toString();
  }

  public void execute(AnActionEvent event, DataContext dataContext, long executionId, @Nullable final ProcessListener processListener) {
    executeIfPossible(event, dataContext, executionId, processListener);
  }

  public boolean executeIfPossible(AnActionEvent event,
                                   DataContext dataContext,
                                   long executionId,
                                   @Nullable final ProcessListener processListener) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return false;
    }

    FileDocumentManager.getInstance().saveAllDocuments();
    try {
      if (isUseConsole()) {
        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(project,
                                                                              DefaultRunExecutor.getRunExecutorInstance(),
                                                                              new ToolRunProfile(this, dataContext)).build();
        if (environment.getState() == null) {
          return false;
        }

        environment.setExecutionId(executionId);
        environment.getRunner().execute(environment, new ProgramRunner.Callback() {
          @Override
          public void processStarted(RunContentDescriptor descriptor) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler != null && processListener != null) {
              LOG.assertTrue(!processHandler.isStartNotified(), "ProcessHandler is already startNotified, the listener won't be correctly notified");
              processHandler.addProcessListener(processListener);
            }
          }
        });
      }
      else {
        GeneralCommandLine commandLine = createCommandLine(dataContext);
        if (commandLine == null) {
          return false;
        }
        OSProcessHandler handler = new OSProcessHandler(commandLine);
        handler.addProcessListener(new ToolProcessAdapter(project, synchronizeAfterExecution(), getName()));
        if (processListener != null) {
          handler.addProcessListener(processListener);
        }
        handler.startNotify();
      }
    }
    catch (ExecutionException ex) {
      ExecutionErrorDialog.show(ex, ToolsBundle.message("tools.process.start.error"), project);
      return false;
    }
    return true;
  }

  @Nullable
  public GeneralCommandLine createCommandLine(DataContext dataContext) {
    if (StringUtil.isEmpty(getWorkingDirectory())) {
      setWorkingDirectory("$ProjectFileDir$");
    }

    GeneralCommandLine commandLine = Registry.is("use.tty.for.external.tools", false)
                                     ? new PtyCommandLine().withConsoleMode(true)
                                     : new GeneralCommandLine();
    try {
      String paramString = MacroManager.getInstance().expandMacrosInString(getParameters(), true, dataContext);
      String workingDir = MacroManager.getInstance().expandMacrosInString(getWorkingDirectory(), true, dataContext);
      String exePath = MacroManager.getInstance().expandMacrosInString(getProgram(), true, dataContext);

      commandLine.getParametersList().addParametersString(
        MacroManager.getInstance().expandMacrosInString(paramString, false, dataContext));
      final String workDirExpanded = MacroManager.getInstance().expandMacrosInString(workingDir, false, dataContext);
      if (!StringUtil.isEmpty(workDirExpanded)) {
        commandLine.setWorkDirectory(workDirExpanded);
      }
      exePath = MacroManager.getInstance().expandMacrosInString(exePath, false, dataContext);
      if (exePath == null) return null;

      File exeFile = new File(exePath);
      if (exeFile.isDirectory() && exeFile.getName().endsWith(".app")) {
        commandLine.setExePath("open");
        commandLine.getParametersList().prependAll("-a", exePath);
      }
      else {
        commandLine.setExePath(exePath);
      }
    }
    catch (Macro.ExecutionCancelledException ignored) {
      return null;
    }
    return ToolsCustomizer.customizeCommandLine(commandLine, dataContext);
  }

  @Override
  public void setGroupName(@NotNull final String name) {
    setGroup(name);
  }

  @Override
  public String getKey() {
    return getName();
  }

  @NotNull
  @Override
  public SchemeElement copy() {
    Tool copy = new Tool();
    copy.copyFrom(this);
    return copy;
  }

  @Override
  public String toString() {
    return myGroup + ": " + myName;
  }

  public String getActionIdPrefix() {
    return ACTION_ID_PREFIX;
  }
}
