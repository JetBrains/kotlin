// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.*;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.terminal.TerminalExecutionConsole;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.tabs.PinToolwindowTabAction;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class RunContentBuilder extends RunTab {
  private static final String JAVA_RUNNER = "JavaRunner";

  private final List<AnAction> myRunnerActions = new SmartList<>();
  private final ExecutionResult myExecutionResult;

  public RunContentBuilder(@NotNull ExecutionResult executionResult, @NotNull ExecutionEnvironment environment) {
    super(environment, getRunnerType(executionResult.getExecutionConsole()));

    myExecutionResult = executionResult;
    myUi.getOptions().setMoveToGridActionEnabled(false).setMinimizeActionEnabled(false);
  }

  @NotNull
  public static ExecutionEnvironment fix(@NotNull ExecutionEnvironment environment, @Nullable ProgramRunner runner) {
    if (runner == null || runner.equals(environment.getRunner())) {
      return environment;
    }
    else {
      return new ExecutionEnvironmentBuilder(environment).runner(runner).build();
    }
  }

  public void addAction(@NotNull final AnAction action) {
    myRunnerActions.add(action);
  }

  @NotNull
  private RunContentDescriptor createDescriptor() {
    final RunProfile profile = myEnvironment.getRunProfile();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return new RunContentDescriptor(profile, myExecutionResult, myUi);
    }

    final ExecutionConsole console = myExecutionResult.getExecutionConsole();
    RunContentDescriptor contentDescriptor = new RunContentDescriptor(profile, myExecutionResult, myUi);
    AnAction[] consoleActionsToMerge = AnAction.EMPTY_ARRAY;
    Content consoleContent = null;
    if (console != null) {
      if (console instanceof ExecutionConsoleEx) {
        ((ExecutionConsoleEx)console).buildUi(myUi);
      }
      else {
        consoleContent = buildConsoleUiDefault(myUi, console);
      }
      initLogConsoles(profile, contentDescriptor, console);
    }
    if (consoleContent != null && myUi.getContentManager().getContentCount() == 1 && console instanceof TerminalExecutionConsole) {
      // TerminalExecutionConsole provides too few toolbar actions. Such console toolbar doesn't look good, but occupy
      // valuable space.
      // Let's show terminal console actions right in the main toolbar iff console content is the only one.
      // Otherwise (with multiple contents), it's unclear what content will be affected by a console action from
      // the main toolbar.
      consoleActionsToMerge = ((TerminalExecutionConsole)console).createConsoleActions();
      // clear console toolbar actions to remove the console toolbar
      consoleContent.setActions(new DefaultActionGroup(), ActionPlaces.RUNNER_TOOLBAR, console.getComponent());
    }
    myUi.getOptions().setLeftToolbar(createActionToolbar(contentDescriptor, consoleActionsToMerge), ActionPlaces.RUNNER_TOOLBAR);

    if (profile instanceof RunConfigurationBase) {
      if (console instanceof ObservableConsoleView && !ApplicationManager.getApplication().isUnitTestMode()) {
        ((ObservableConsoleView)console).addChangeListener(new ConsoleToFrontListener((RunConfigurationBase)profile,
                                                                                      myProject,
                                                                                      myEnvironment.getExecutor(),
                                                                                      contentDescriptor,
                                                                                      myUi),
                                                           this);
      }
    }

    return contentDescriptor;
  }

  @NotNull
  private static String getRunnerType(@Nullable ExecutionConsole console) {
    if (console instanceof ExecutionConsoleEx) {
      String id = ((ExecutionConsoleEx)console).getExecutionConsoleId();
      if (id != null) {
        return JAVA_RUNNER + '.' + id;
      }
    }
    return JAVA_RUNNER;
  }

  @NotNull
  public static Content buildConsoleUiDefault(@NotNull RunnerLayoutUi ui, @NotNull ExecutionConsole console) {
    Content consoleContent = ui.createContent(ExecutionConsole.CONSOLE_CONTENT_ID, console.getComponent(), "Console",
                                              AllIcons.Debugger.Console,
                                              console.getPreferredFocusableComponent());

    consoleContent.setCloseable(false);
    addAdditionalConsoleEditorActions(console, consoleContent);
    ui.addContent(consoleContent, 0, PlaceInGrid.bottom, false);
    return consoleContent;
  }

  public static void addAdditionalConsoleEditorActions(final ExecutionConsole console, final Content consoleContent) {
    final DefaultActionGroup consoleActions = new DefaultActionGroup();
    if (console instanceof ConsoleView) {
      for (AnAction action : ((ConsoleView)console).createConsoleActions()) {
        consoleActions.add(action);
      }
    }

    consoleContent.setActions(consoleActions, ActionPlaces.RUNNER_TOOLBAR, console.getComponent());
  }

  @NotNull
  private ActionGroup createActionToolbar(@NotNull RunContentDescriptor contentDescriptor, @NotNull AnAction[] consoleActions) {
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_RERUN));
    final AnAction[] actions = contentDescriptor.getRestartActions();
    actionGroup.addAll(actions);
    if (actions.length > 0) {
      actionGroup.addSeparator();
    }


    actionGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM));
    actionGroup.addAll(myExecutionResult.getActions());
    if (consoleActions.length > 0) {
      actionGroup.addSeparator();
      actionGroup.addAll(consoleActions);
    }

    for (AnAction anAction : myRunnerActions) {
      if (anAction != null) {
        actionGroup.add(anAction);
      }
      else {
        actionGroup.addSeparator();
      }
    }

    actionGroup.addSeparator();
    actionGroup.add(myUi.getOptions().getLayoutActions());
    actionGroup.addSeparator();
    actionGroup.add(PinToolwindowTabAction.getPinAction());
    return actionGroup;
  }

  /**
   * @param reuseContent see {@link RunContentDescriptor#myContent}
   */
  public RunContentDescriptor showRunContent(@Nullable RunContentDescriptor reuseContent) {
    RunContentDescriptor descriptor = createDescriptor();
    Disposer.register(descriptor, this);
    Disposer.register(myProject, descriptor);
    RunContentManagerImpl.copyContentAndBehavior(descriptor, reuseContent);
    myRunContentDescriptor = descriptor;
    return descriptor;
  }

  public static class ConsoleToFrontListener implements ConsoleViewImpl.ChangeListener {
    @NotNull private final RunConfigurationBase myRunConfigurationBase;
    @NotNull private final Project myProject;
    @NotNull private final Executor myExecutor;
    @NotNull private final RunContentDescriptor myRunContentDescriptor;
    @NotNull private final RunnerLayoutUi myUi;

    public ConsoleToFrontListener(@NotNull RunConfigurationBase runConfigurationBase,
                                  @NotNull Project project,
                                  @NotNull Executor executor,
                                  @NotNull RunContentDescriptor runContentDescriptor,
                                  @NotNull RunnerLayoutUi ui) {
      myRunConfigurationBase = runConfigurationBase;
      myProject = project;
      myExecutor = executor;
      myRunContentDescriptor = runContentDescriptor;
      myUi = ui;
    }

    @Override
    public void contentAdded(@NotNull Collection<ConsoleViewContentType> types) {
      if (myProject.isDisposed() || myUi.isDisposed())
        return;
      for (ConsoleViewContentType type : types) {
        if ((type == ConsoleViewContentType.NORMAL_OUTPUT) && myRunConfigurationBase.isShowConsoleOnStdOut()
            || (type == ConsoleViewContentType.ERROR_OUTPUT) && myRunConfigurationBase.isShowConsoleOnStdErr()) {
          ExecutionManager.getInstance(myProject).getContentManager().toFrontRunContent(myExecutor, myRunContentDescriptor);
          myUi.selectAndFocus(myUi.findContent(ExecutionConsole.CONSOLE_CONTENT_ID), false, false);
          return;
        }
      }
    }
  }

  public GlobalSearchScope getSearchScope() {
    return mySearchScope;
  }
}
