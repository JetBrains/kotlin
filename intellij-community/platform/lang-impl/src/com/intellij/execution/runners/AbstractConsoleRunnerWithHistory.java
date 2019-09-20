// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.console.ConsoleExecuteAction;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.ide.CommonActionsManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides basic functionality for running consoles.
 * It launches external process and handles line input with history
 *
 * @author oleg
 */
public abstract class AbstractConsoleRunnerWithHistory<T extends LanguageConsoleView> {
  private final String myConsoleTitle;

  private ProcessHandler myProcessHandler;
  private final String myWorkingDir;

  private T myConsoleView;

  @NotNull
  private final Project myProject;

  private ProcessBackedConsoleExecuteActionHandler myConsoleExecuteActionHandler;

  public AbstractConsoleRunnerWithHistory(@NotNull Project project, @NotNull String consoleTitle, @Nullable String workingDir) {
    myProject = project;
    myConsoleTitle = consoleTitle;
    myWorkingDir = workingDir;
  }

  /**
   * Launch process, setup history, actions etc.
   *
   * @throws ExecutionException
   */
  public void initAndRun() throws ExecutionException {
    // Create Server process
    final Process process = createProcess();
    UIUtil.invokeLaterIfNeeded(() -> {
      // Init console view
      myConsoleView = createConsoleView();
      if (myConsoleView instanceof JComponent) {
        ((JComponent)myConsoleView).setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT));
      }
      myProcessHandler = createProcessHandler(process);

      myConsoleExecuteActionHandler = createExecuteActionHandler();

      ProcessTerminatedListener.attach(myProcessHandler);

      myProcessHandler.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          finishConsole();
        }
      });

      // Attach to process
      myConsoleView.attachToProcess(myProcessHandler);

      // Runner creating
      createContentDescriptorAndActions();

      // Run
      myProcessHandler.startNotify();
    });
  }

  protected Executor getExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }

  protected void createContentDescriptorAndActions() {
    final Executor defaultExecutor = getExecutor();
    final DefaultActionGroup toolbarActions = new DefaultActionGroup();
    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ConsoleRunner", toolbarActions, false);

    // Runner creating
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(actionToolbar.getComponent(), BorderLayout.WEST);
    panel.add(myConsoleView.getComponent(), BorderLayout.CENTER);

    actionToolbar.setTargetComponent(panel);

    final RunContentDescriptor contentDescriptor =
      new RunContentDescriptor(myConsoleView, myProcessHandler, panel, constructConsoleTitle(myConsoleTitle), getConsoleIcon());

    contentDescriptor.setFocusComputable(() -> getConsoleView().getConsoleEditor().getContentComponent());
    contentDescriptor.setAutoFocusContent(isAutoFocusContent());


    // tool bar actions
    final List<AnAction> actions = fillToolBarActions(toolbarActions, defaultExecutor, contentDescriptor);
    registerActionShortcuts(actions, getConsoleView().getConsoleEditor().getComponent());
    registerActionShortcuts(actions, panel);

    showConsole(defaultExecutor, contentDescriptor);
  }

  @Nullable
  protected Icon getConsoleIcon() {
    return null;
  }

  protected String constructConsoleTitle(final @NotNull String consoleTitle) {
    return new ConsoleTitleGen(myProject, consoleTitle, shouldAddNumberToTitle()).makeTitle();
  }

  public boolean isAutoFocusContent() {
    return true;
  }

  protected boolean shouldAddNumberToTitle() {
    return false;
  }

  protected void showConsole(Executor defaultExecutor, @NotNull RunContentDescriptor contentDescriptor) {
    // Show in run toolwindow
    ExecutionManager.getInstance(myProject).getContentManager().showRunContent(defaultExecutor, contentDescriptor);
  }

  protected void finishConsole() {
    myConsoleView.setEditable(false);
  }

  protected abstract T createConsoleView();

  @Nullable
  protected abstract Process createProcess() throws ExecutionException;

  protected abstract OSProcessHandler createProcessHandler(final Process process);

  public static void registerActionShortcuts(final List<? extends AnAction> actions, final JComponent component) {
    for (AnAction action : actions) {
      action.registerCustomShortcutSet(action.getShortcutSet(), component);
    }
  }

  protected List<AnAction> fillToolBarActions(final DefaultActionGroup toolbarActions,
                                              final Executor defaultExecutor,
                                              final RunContentDescriptor contentDescriptor) {

    List<AnAction> actionList = new ArrayList<>();

    //stop
    actionList.add(createStopAction());

    //close
    actionList.add(createCloseAction(defaultExecutor, contentDescriptor));

    // run action
    actionList.add(createConsoleExecAction(myConsoleExecuteActionHandler));

    // Help
    actionList.add(CommonActionsManager.getInstance().createHelpAction("interactive_console"));

    toolbarActions.addAll(actionList);

    return actionList;
  }

  protected AnAction createCloseAction(final Executor defaultExecutor, final RunContentDescriptor myDescriptor) {
    return new CloseAction(defaultExecutor, myDescriptor, myProject);
  }

  protected AnAction createStopAction() {
    return ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM);
  }

  protected AnAction createConsoleExecAction(@NotNull ProcessBackedConsoleExecuteActionHandler consoleExecuteActionHandler) {
    String emptyAction = consoleExecuteActionHandler.getEmptyExecuteAction();
    return new ConsoleExecuteAction(myConsoleView, consoleExecuteActionHandler, emptyAction, consoleExecuteActionHandler);
  }

  @NotNull
  protected abstract ProcessBackedConsoleExecuteActionHandler createExecuteActionHandler();

  public T getConsoleView() {
    return myConsoleView;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public String getConsoleTitle() {
    return myConsoleTitle;
  }

  public String getWorkingDir() {
    return myWorkingDir;
  }

  public ProcessHandler getProcessHandler() {
    return myProcessHandler;
  }

  public ProcessBackedConsoleExecuteActionHandler getConsoleExecuteActionHandler() {
    return myConsoleExecuteActionHandler;
  }


}
