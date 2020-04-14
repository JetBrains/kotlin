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

import com.intellij.build.process.BuildProcessHandler;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
public class DefaultBuildDescriptor implements BuildDescriptor {

  private final Object myId;
  private final String myTitle;
  private final String myWorkingDir;
  private final long myStartTime;

  private boolean myActivateToolWindowWhenAdded;
  private boolean myActivateToolWindowWhenFailed = true;
  private boolean myAutoFocusContent = false;

  private final @NotNull List<AnAction> myActions = new SmartList<>();
  private final @NotNull List<AnAction> myRestartActions = new SmartList<>();
  private final @NotNull List<Filter> myExecutionFilters = new SmartList<>();
  private final @NotNull List<Function<ExecutionNode, AnAction>> myContextActions = new SmartList<>();

  private @Nullable BuildProcessHandler myProcessHandler;
  private @Nullable Consumer<ConsoleView> myAttachedConsoleConsumer;
  private @Nullable ExecutionEnvironment myExecutionEnvironment;
  private @Nullable Supplier<RunContentDescriptor> myContentDescriptorSupplier;

  public DefaultBuildDescriptor(@NotNull Object id,
                                @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title,
                                @NotNull String workingDir,
                                long startTime) {
    myId = id;
    myTitle = title;
    myWorkingDir = workingDir;
    myStartTime = startTime;
  }

  public DefaultBuildDescriptor(@Nullable @NotNull BuildDescriptor descriptor) {
    this(descriptor.getId(), descriptor.getTitle(), descriptor.getWorkingDir(), descriptor.getStartTime());
    if (descriptor instanceof DefaultBuildDescriptor) {
      DefaultBuildDescriptor defaultBuildDescriptor = (DefaultBuildDescriptor)descriptor;
      myActivateToolWindowWhenAdded = defaultBuildDescriptor.myActivateToolWindowWhenAdded;
      myActivateToolWindowWhenFailed = defaultBuildDescriptor.myActivateToolWindowWhenFailed;
      myAutoFocusContent = defaultBuildDescriptor.myAutoFocusContent;

      defaultBuildDescriptor.myRestartActions.forEach(this::withRestartAction);
      defaultBuildDescriptor.myActions.forEach(this::withAction);
      defaultBuildDescriptor.myExecutionFilters.forEach(this::withExecutionFilter);
      defaultBuildDescriptor.myContextActions.forEach(this::withContextAction);

      myContentDescriptorSupplier = defaultBuildDescriptor.myContentDescriptorSupplier;
      myExecutionEnvironment = defaultBuildDescriptor.myExecutionEnvironment;
      myProcessHandler = defaultBuildDescriptor.myProcessHandler;
      myAttachedConsoleConsumer = defaultBuildDescriptor.myAttachedConsoleConsumer;
    }
  }

  @NotNull
  @Override
  public Object getId() {
    return myId;
  }

  @NotNull
  @Override
  public String getTitle() {
    return myTitle;
  }

  @NotNull
  @Override
  public String getWorkingDir() {
    return myWorkingDir;
  }

  @Override
  public long getStartTime() {
    return myStartTime;
  }

  @ApiStatus.Experimental
  @NotNull
  public List<AnAction> getActions() {
    return Collections.unmodifiableList(myActions);
  }

  @ApiStatus.Experimental
  @NotNull
  public List<AnAction> getRestartActions() {
    return Collections.unmodifiableList(myRestartActions);
  }

  @ApiStatus.Experimental
  @NotNull
  public List<AnAction> getContextActions(@NotNull ExecutionNode node) {
    return ContainerUtil.map(myContextActions, function -> function.apply(node));
  }

  @ApiStatus.Experimental
  @NotNull
  public List<Filter> getExecutionFilters() {
    return Collections.unmodifiableList(myExecutionFilters);
  }

  public boolean isActivateToolWindowWhenAdded() {
    return myActivateToolWindowWhenAdded;
  }

  public void setActivateToolWindowWhenAdded(boolean activateToolWindowWhenAdded) {
    myActivateToolWindowWhenAdded = activateToolWindowWhenAdded;
  }

  public boolean isActivateToolWindowWhenFailed() {
    return myActivateToolWindowWhenFailed;
  }

  public void setActivateToolWindowWhenFailed(boolean activateToolWindowWhenFailed) {
    myActivateToolWindowWhenFailed = activateToolWindowWhenFailed;
  }

  public boolean isAutoFocusContent() {
    return myAutoFocusContent;
  }

  public void setAutoFocusContent(boolean autoFocusContent) {
    myAutoFocusContent = autoFocusContent;
  }

  @Nullable
  public BuildProcessHandler getProcessHandler() {
    return myProcessHandler;
  }

  @Nullable
  public ExecutionEnvironment getExecutionEnvironment() {
    return myExecutionEnvironment;
  }

  @Nullable
  public Supplier<RunContentDescriptor> getContentDescriptorSupplier() {
    return myContentDescriptorSupplier;
  }

  @Nullable
  public Consumer<ConsoleView> getAttachedConsoleConsumer() {
    return myAttachedConsoleConsumer;
  }

  @ApiStatus.Experimental
  public DefaultBuildDescriptor withAction(@NotNull AnAction action) {
    myActions.add(action);
    return this;
  }

  @ApiStatus.Experimental
  public DefaultBuildDescriptor withRestartAction(@NotNull AnAction action) {
    myRestartActions.add(action);
    return this;
  }

  @ApiStatus.Experimental
  public DefaultBuildDescriptor withRestartActions(AnAction @NotNull ... actions) {
    myRestartActions.addAll(Arrays.asList(actions));
    return this;
  }

  @ApiStatus.Experimental
  public DefaultBuildDescriptor withContextAction(Function<ExecutionNode, AnAction> contextAction) {
    myContextActions.add(contextAction);
    return this;
  }

  @ApiStatus.Experimental
  public DefaultBuildDescriptor withContextActions(AnAction @NotNull ... actions) {
    for (AnAction action : actions) {
      myContextActions.add(node -> action);
    }
    return this;
  }

  @ApiStatus.Experimental
  public DefaultBuildDescriptor withExecutionFilter(@NotNull Filter filter) {
    myExecutionFilters.add(filter);
    return this;
  }

  public DefaultBuildDescriptor withContentDescriptor(Supplier<RunContentDescriptor> contentDescriptorSupplier) {
    myContentDescriptorSupplier = contentDescriptorSupplier;
    return this;
  }

  public DefaultBuildDescriptor withProcessHandler(@Nullable BuildProcessHandler processHandler,
                                                   @Nullable Consumer<ConsoleView> attachedConsoleConsumer) {
    myProcessHandler = processHandler;
    myAttachedConsoleConsumer = attachedConsoleConsumer;
    return this;
  }

  public DefaultBuildDescriptor withExecutionEnvironment(@Nullable ExecutionEnvironment env) {
    myExecutionEnvironment = env;
    return this;
  }
}
