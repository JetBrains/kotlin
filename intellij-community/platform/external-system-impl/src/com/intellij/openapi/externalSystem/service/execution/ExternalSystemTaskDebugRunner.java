/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.build.BuildView;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.ServerSocket;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemTaskDebugRunner extends GenericDebuggerRunner {
  static final Logger LOG = Logger.getInstance(ExternalSystemTaskDebugRunner.class);

  @NotNull
  @Override
  public String getRunnerId() {
    return ExternalSystemConstants.DEBUG_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return profile instanceof ExternalSystemRunConfiguration && DefaultDebugExecutor.EXECUTOR_ID.equals(executorId);
  }

  @Nullable
  @Override
  protected RunContentDescriptor createContentDescriptor(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    if (state instanceof ExternalSystemRunConfiguration.MyRunnableState) {
      ExternalSystemRunConfiguration.MyRunnableState myRunnableState = (ExternalSystemRunConfiguration.MyRunnableState)state;
      int port = myRunnableState.getDebugPort();
      if (port > 0) {
        RunContentDescriptor runContentDescriptor = doGetRunContentDescriptor(myRunnableState, environment, port);
        if (runContentDescriptor == null) return null;

        ProcessHandler processHandler = runContentDescriptor.getProcessHandler();
        final ServerSocket socket = myRunnableState.getForkSocket();
        if (socket != null && processHandler != null) {
          new ForkedDebuggerThread(processHandler, runContentDescriptor, socket, environment.getProject()).start();
        }
        return runContentDescriptor;
      }
      else {
        LOG.warn("Can't attach debugger to external system task execution. Reason: target debug port is unknown");
      }
    }
    else {
      LOG.warn(String.format(
        "Can't attach debugger to external system task execution. Reason: invalid run profile state is provided"
        + "- expected '%s' but got '%s'",
        ExternalSystemRunConfiguration.MyRunnableState.class.getName(), state.getClass().getName()
      ));
    }
    return null;
  }

  @Nullable
  private RunContentDescriptor doGetRunContentDescriptor(@NotNull ExternalSystemRunConfiguration.MyRunnableState state,
                                                         @NotNull ExecutionEnvironment environment,
                                                         int port) throws ExecutionException {
    RemoteConnection connection = new RemoteConnection(true, "127.0.0.1", String.valueOf(port), true);
    RunContentDescriptor runContentDescriptor = attachVirtualMachine(state, environment, connection, true);
    if (runContentDescriptor == null) return null;

    state.setContentDescriptor(runContentDescriptor);

    ExecutionConsole executionConsole = runContentDescriptor.getExecutionConsole();
    if (executionConsole instanceof BuildView) {
      return runContentDescriptor;
    }
    RunContentDescriptor descriptor =
      new RunContentDescriptor(runContentDescriptor.getExecutionConsole(), runContentDescriptor.getProcessHandler(),
                               runContentDescriptor.getComponent(), runContentDescriptor.getDisplayName(),
                               runContentDescriptor.getIcon(), null,
                               runContentDescriptor.getRestartActions()) {
        @Override
        public boolean isHiddenContent() {
          return true;
        }
      };
    descriptor.setRunnerLayoutUi(runContentDescriptor.getRunnerLayoutUi());
    return descriptor;
  }
}
