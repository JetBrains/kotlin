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
package com.intellij.build;

import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.OutputBuildEvent;
import com.intellij.build.events.StartBuildEvent;
import com.intellij.build.events.impl.StartBuildEventImpl;
import com.intellij.build.process.BuildProcessHandler;
import com.intellij.execution.actions.StopAction;
import com.intellij.execution.actions.StopProcessAction;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.FakeRerunAction;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.actions.PinActiveTabAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class BuildView extends CompositeView<ExecutionConsole>
  implements BuildProgressListener, ConsoleView, DataProvider, Filterable<ExecutionNode>, OccurenceNavigator {
  public static final String CONSOLE_VIEW_NAME = "consoleView";
  private final AtomicReference<StartBuildEvent> myStartBuildEventRef = new AtomicReference<>();
  private final BuildDescriptor myBuildDescriptor;
  private final Project myProject;
  private final AtomicBoolean isBuildStartEventProcessed = new AtomicBoolean();
  private final List<BuildEvent> myAfterStartEvents = ContainerUtil.createConcurrentList();
  private final ViewManager myViewManager;
  @Nullable private volatile ExecutionConsole myExecutionConsole;
  private volatile BuildViewSettingsProvider myViewSettingsProvider;

  public BuildView(Project project, BuildDescriptor buildDescriptor, String selectionStateKey, ViewManager viewManager) {
    this(project, null, buildDescriptor, selectionStateKey, viewManager);
  }

  public BuildView(Project project,
                   @Nullable ExecutionConsole executionConsole,
                   BuildDescriptor buildDescriptor,
                   String selectionStateKey,
                   ViewManager viewManager) {
    super(selectionStateKey);
    myProject = project;
    myBuildDescriptor = buildDescriptor;
    myViewManager = viewManager;
    myExecutionConsole = executionConsole;
  }

  @Override
  public void onEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    if (event instanceof StartBuildEvent) {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        onStartBuild(buildId, (StartBuildEvent)event);
        for (BuildEvent buildEvent : myAfterStartEvents) {
          processEvent(buildId, buildEvent);
        }
        myAfterStartEvents.clear();
        isBuildStartEventProcessed.set(true);
      });
      return;
    }

    if (!isBuildStartEventProcessed.get()) {
      myAfterStartEvents.add(event);
    }
    else {
      processEvent(buildId, event);
    }
  }

  private void processEvent(@NotNull Object buildId, @NotNull BuildEvent event) {
    if (event instanceof OutputBuildEvent && (event.getParentId() == null || event.getParentId() == myBuildDescriptor.getId())) {
      ExecutionConsole consoleView = getConsoleView();
      if (consoleView instanceof BuildProgressListener) {
        ((BuildProgressListener)consoleView).onEvent(buildId, event);
      }
    }
    else {
      BuildTreeConsoleView eventView = getEventView();
      if (eventView != null) {
        EdtExecutorService.getInstance().execute(() -> eventView.onEvent(buildId, event));
      }
    }
  }

  private void onStartBuild(@NotNull Object buildId, @NotNull StartBuildEvent startBuildEvent) {
    myStartBuildEventRef.set(startBuildEvent);
    if (startBuildEvent instanceof StartBuildEventImpl) {
      myViewSettingsProvider = ((StartBuildEventImpl)startBuildEvent).getBuildViewSettingsProvider();
    }
    if (myViewSettingsProvider == null) {
      myViewSettingsProvider = () -> false;
    }
    if (myExecutionConsole == null) {
      Supplier<RunContentDescriptor> descriptorSupplier = startBuildEvent.getContentDescriptorSupplier();
      RunContentDescriptor runContentDescriptor = descriptorSupplier != null ? descriptorSupplier.get() : null;
      myExecutionConsole = runContentDescriptor != null &&
                             runContentDescriptor.getExecutionConsole() != null &&
                             runContentDescriptor.getExecutionConsole() != this ?
                           runContentDescriptor.getExecutionConsole() : new BuildTextConsoleView(myProject);
      if (runContentDescriptor != null && Disposer.findRegisteredObject(runContentDescriptor, this) == null) {
        Disposer.register(this, runContentDescriptor);
      }
    }
    if (myExecutionConsole != null) {
      myExecutionConsole.getComponent(); //create editor to be able to add console editor actions
      if (myViewSettingsProvider.isExecutionViewHidden() || !myViewSettingsProvider.isSideBySideView()) {
        addView(myExecutionConsole, CONSOLE_VIEW_NAME, myViewManager.isConsoleEnabledByDefault());
      }
    }

    BuildTreeConsoleView eventView = null;
    if (!myViewSettingsProvider.isExecutionViewHidden()) {
      eventView = getEventView();
      if (eventView == null) {
        String eventViewName = BuildTreeConsoleView.class.getName();
        eventView = new BuildTreeConsoleView(myProject, myBuildDescriptor,
                                             myExecutionConsole,
                                             myViewSettingsProvider);
        addView(eventView, eventViewName, myViewSettingsProvider.isSideBySideView() || !myViewManager.isConsoleEnabledByDefault());
      }
    }

    BuildProcessHandler processHandler = startBuildEvent.getProcessHandler();
    if (myExecutionConsole instanceof ConsoleView) {
      for (Filter filter : startBuildEvent.getExecutionFilters()) {
        ((ConsoleView)myExecutionConsole).addMessageFilter(filter);
      }

      if (processHandler != null) {
        ((ConsoleView)myExecutionConsole).attachToProcess(processHandler);
        Consumer<ConsoleView> attachedConsoleConsumer = startBuildEvent.getAttachedConsoleConsumer();
        if (attachedConsoleConsumer != null) {
          attachedConsoleConsumer.consume((ConsoleView)myExecutionConsole);
        }
        if (!processHandler.isStartNotified()) {
          processHandler.startNotify();
        }
      }
    }
    if (processHandler != null && !processHandler.isStartNotified()) {
      processHandler.startNotify();
    }

    if (eventView != null) {
      eventView.onEvent(buildId, startBuildEvent);
    }
  }

  @Nullable
  @ApiStatus.Internal
  ExecutionConsole getConsoleView() {
    return myExecutionConsole;
  }

  @Nullable
  @ApiStatus.Internal
  BuildTreeConsoleView getEventView() {
    return getView(BuildTreeConsoleView.class.getName(), BuildTreeConsoleView.class);
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
    delegateToConsoleView(view -> view.print(text, contentType));
  }

  private void delegateToConsoleView(Consumer<? super ConsoleView> viewConsumer) {
    ExecutionConsole console = getConsoleView();
    if (console instanceof ConsoleView) {
      viewConsumer.consume((ConsoleView)console);
    }
  }

  @Nullable
  private <R> R getConsoleViewValue(Function<? super ConsoleView, ? extends R> viewConsumer) {
    ExecutionConsole console = getConsoleView();
    if (console instanceof ConsoleView) {
      return viewConsumer.apply((ConsoleView)console);
    }
    return null;
  }

  @Override
  public void clear() {
    delegateToConsoleView(ConsoleView::clear);
  }

  @Override
  public void scrollTo(int offset) {
    delegateToConsoleView(view -> view.scrollTo(offset));
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
    delegateToConsoleView(view -> view.attachToProcess(processHandler));
  }

  @Override
  public void setOutputPaused(boolean value) {
    delegateToConsoleView(view -> view.setOutputPaused(value));
  }

  @Override
  public boolean isOutputPaused() {
    Boolean result = getConsoleViewValue(ConsoleView::isOutputPaused);
    return result != null && result;
  }

  @Override
  public boolean hasDeferredOutput() {
    Boolean result = getConsoleViewValue(ConsoleView::hasDeferredOutput);
    return result != null && result;
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
    delegateToConsoleView(view -> view.performWhenNoDeferredOutput(runnable));
  }

  @Override
  public void setHelpId(@NotNull String helpId) {
    delegateToConsoleView(view -> view.setHelpId(helpId));
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
    delegateToConsoleView(view -> view.addMessageFilter(filter));
  }

  @Override
  public void printHyperlink(@NotNull String hyperlinkText, @Nullable HyperlinkInfo info) {
    delegateToConsoleView(view -> view.printHyperlink(hyperlinkText, info));
  }

  @Override
  public int getContentSize() {
    Integer result = getConsoleViewValue(ConsoleView::getContentSize);
    return result == null ? 0 : result;
  }

  @Override
  public boolean canPause() {
    Boolean result = getConsoleViewValue(ConsoleView::canPause);
    return result != null && result;
  }

  @NotNull
  @Override
  public AnAction[] createConsoleActions() {
    final DefaultActionGroup rerunActionGroup = new DefaultActionGroup();
    AnAction stopAction = null;
    StartBuildEvent startBuildEvent = myStartBuildEventRef.get();
    if (startBuildEvent != null && startBuildEvent.getProcessHandler() != null) {
      stopAction = new StopProcessAction("Stop", "Stop", startBuildEvent.getProcessHandler());
      ActionUtil.copyFrom(stopAction, IdeActions.ACTION_STOP_PROGRAM);
      stopAction.registerCustomShortcutSet(stopAction.getShortcutSet(), this);
    }
    final DefaultActionGroup consoleActionGroup = new DefaultActionGroup() {
      @Override
      public void update(@NotNull AnActionEvent e) {
        super.update(e);
        String eventViewName = BuildTreeConsoleView.class.getName();
        e.getPresentation().setVisible(myViewSettingsProvider != null && !myViewSettingsProvider.isSideBySideView()
                                       && !BuildView.this.isViewEnabled(eventViewName));
      }
    };

    ExecutionConsole consoleView = getConsoleView();
    if (consoleView instanceof ConsoleView) {
      consoleView.getComponent(); //create editor to be able to add console editor actions
      final AnAction[] consoleActions = ((ConsoleView)consoleView).createConsoleActions();
      for (AnAction anAction : consoleActions) {
        if (anAction instanceof StopAction) {
          if (stopAction == null) {
            stopAction = anAction;
          }
        }
        else if (!(anAction instanceof FakeRerunAction ||
                   anAction instanceof PinActiveTabAction ||
                   anAction instanceof CloseAction)) {
          consoleActionGroup.add(anAction);
        }
      }
    }
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    if (startBuildEvent != null) {
      for (AnAction anAction : startBuildEvent.getRestartActions()) {
        rerunActionGroup.add(anAction);
      }
    }
    if (stopAction != null) {
      rerunActionGroup.add(stopAction);
    }
    actionGroup.add(rerunActionGroup);
    if (myViewManager.isBuildContentView() && (myViewSettingsProvider == null || !myViewSettingsProvider.isSideBySideView())) {
      actionGroup.addAll(getSwitchActions());
      actionGroup.addSeparator();
    }
    return new AnAction[]{actionGroup, consoleActionGroup};
  }

  @Override
  public void allowHeavyFilters() {
    delegateToConsoleView(ConsoleView::allowHeavyFilters);
  }

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    if (LangDataKeys.CONSOLE_VIEW.is(dataId)) {
      return getConsoleView();
    }
    Object data = super.getData(dataId);
    if (data != null) return data;
    StartBuildEvent startBuildEvent = myStartBuildEventRef.get();
    if (startBuildEvent != null && LangDataKeys.RUN_PROFILE.is(dataId)) {
      ExecutionEnvironment environment = startBuildEvent.getExecutionEnvironment();
      return environment == null ? null : environment.getRunProfile();
    }
    if (startBuildEvent != null && LangDataKeys.EXECUTION_ENVIRONMENT.is(dataId)) {
      return startBuildEvent.getExecutionEnvironment();
    }
    return null;
  }

  @Override
  public boolean isFilteringEnabled() {
    return getEventView() != null;
  }

  @NotNull
  @Override
  public Predicate<ExecutionNode> getFilter() {
    BuildTreeConsoleView eventView = getEventView();
    return eventView == null ? executionNode -> true : eventView.getFilter();
  }

  @Override
  public void addFilter(@NotNull Predicate<ExecutionNode> filter) {
    BuildTreeConsoleView eventView = getEventView();
    if (eventView != null) {
      eventView.addFilter(filter);
    }
  }

  @Override
  public void removeFilter(@NotNull Predicate<ExecutionNode> filter) {
    BuildTreeConsoleView eventView = getEventView();
    if (eventView != null) {
      eventView.removeFilter(filter);
    }
  }

  @Override
  public boolean contains(@NotNull Predicate<ExecutionNode> filter) {
    BuildTreeConsoleView eventView = getEventView();
    return eventView != null && eventView.contains(filter);
  }

  @NotNull
  private OccurenceNavigator getOccurenceNavigator() {
    BuildTreeConsoleView eventView = getEventView();
    if (eventView != null) return eventView;
    ExecutionConsole executionConsole = getConsoleView();
    if (executionConsole instanceof OccurenceNavigator) {
      return (OccurenceNavigator)executionConsole;
    }
    return EMPTY;
  }

  @Override
  public boolean hasNextOccurence() {
    return getOccurenceNavigator().hasNextOccurence();
  }

  @Override
  public boolean hasPreviousOccurence() {
    return getOccurenceNavigator().hasPreviousOccurence();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return getOccurenceNavigator().goNextOccurence();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return getOccurenceNavigator().goPreviousOccurence();
  }

  @NotNull
  @Override
  public String getNextOccurenceActionName() {
    return getOccurenceNavigator().getNextOccurenceActionName();
  }

  @NotNull
  @Override
  public String getPreviousOccurenceActionName() {
    return getOccurenceNavigator().getPreviousOccurenceActionName();
  }
}
