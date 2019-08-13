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
package com.intellij.tools;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.util.concurrency.Semaphore;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author traff
 */
public abstract class AbstractToolBeforeRunTask<ToolBeforeRunTask extends AbstractToolBeforeRunTask, T extends Tool>
  extends BeforeRunTask<ToolBeforeRunTask> {
  @NonNls private final static String ACTION_ID_ATTRIBUTE = "actionId";
  private static final Logger LOG = Logger.getInstance(AbstractToolBeforeRunTask.class);
  protected String myToolActionId;

  public AbstractToolBeforeRunTask(Key<ToolBeforeRunTask> providerId) {
    super(providerId);
  }

  @Nullable
  public String getToolActionId() {
    return myToolActionId;
  }

  @Override
  public void writeExternal(@NotNull Element element) {
    super.writeExternal(element);
    if (myToolActionId != null) {
      element.setAttribute(ACTION_ID_ATTRIBUTE, myToolActionId);
    }
  }

  @Override
  public void readExternal(@NotNull Element element) {
    super.readExternal(element);
    myToolActionId = element.getAttributeValue(ACTION_ID_ATTRIBUTE);
  }

  @Override
  public ToolBeforeRunTask clone() {
    return (ToolBeforeRunTask)super.clone();
  }

  public void setToolActionId(String toolActionId) {
    myToolActionId = toolActionId;
  }

  public boolean isExecutable() {
    return myToolActionId != null;
  }

  public boolean execute(final DataContext context, final long executionId) {
    T tool = findCorrespondingTool();
    if (tool != null && !tool.isEnabled()) {
      return true;
    }
    
    final Semaphore targetDone = new Semaphore();
    final Ref<Boolean> result = new Ref<>(false);

    try {
      ApplicationManager.getApplication().invokeAndWait(() -> ToolAction.runTool(myToolActionId, context, null, executionId, new ProcessAdapter() {
        @Override
        public void startNotified(@NotNull final ProcessEvent event) {
          targetDone.down();
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          result.set(event.getExitCode() == 0);
          targetDone.up();
        }
      }), ModalityState.NON_MODAL);
    }
    catch (Exception e) {
      LOG.error(e);
      return false;
    }
    targetDone.waitFor();
    return result.get();
  }

  @Nullable
  public T findCorrespondingTool() {
    if (myToolActionId == null) {
      return null;
    }
    List<T> tools = getTools();
    for (T tool : tools) {
      if (myToolActionId.equals(tool.getActionId())) {
        return tool;
      }
    }
    return null;
  }

  protected abstract List<T> getTools();
}
