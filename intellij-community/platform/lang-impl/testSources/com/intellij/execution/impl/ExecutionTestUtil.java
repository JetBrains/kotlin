// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.List;
import java.util.Objects;

public class ExecutionTestUtil {
  private ExecutionTestUtil() {
  }

  @NotNull
  public static RunContentDescriptor getSingleRunContentDescriptor(@NotNull ExecutionManager executionManager) {
    List<RunContentDescriptor> descriptors = ((ExecutionManagerImpl)executionManager).getRunningDescriptors(Conditions.alwaysTrue());
    String actualDescriptorsMsg = stringifyDescriptors(descriptors);
    Assert.assertEquals(actualDescriptorsMsg, 1, descriptors.size());
    RunContentDescriptor descriptor = ContainerUtil.getFirstItem(descriptors);
    return Objects.requireNonNull(descriptor);
  }

  public static void terminateAllRunningDescriptors(@NotNull ExecutionManager executionManager) {
    List<RunContentDescriptor> descriptors = ((ExecutionManagerImpl)executionManager).getRunningDescriptors(Conditions.alwaysTrue());
    for (RunContentDescriptor descriptor : descriptors) {
      ProcessHandler processHandler = descriptor.getProcessHandler();
      if (processHandler != null) {
        ScriptRunnerUtil.terminateProcessHandler(processHandler, 0, null);
      }
    }
  }

  @NotNull
  private static String stringifyDescriptors(@NotNull List<? extends RunContentDescriptor> descriptors) {
    return "Actual descriptors: " + StringUtil.join(descriptors, descriptor -> {
      if (descriptor == null) {
        return "null";
      }
      ProcessHandler processHandler = descriptor.getProcessHandler();
      return String.format("[%s, %s]",
                           descriptor.getDisplayName(),
                           processHandler != null ? processHandler.getClass().getName() : null);
    }, ", ");
  }
}
