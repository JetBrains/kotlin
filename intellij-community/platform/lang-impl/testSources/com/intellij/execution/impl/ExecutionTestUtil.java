/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.List;

public class ExecutionTestUtil {
  private ExecutionTestUtil() {
  }

  @NotNull
  public static RunContentDescriptor getSingleRunContentDescriptor(@NotNull ExecutionManager executionManager) {
    List<RunContentDescriptor> descriptors = ((ExecutionManagerImpl)executionManager).getRunningDescriptors(Conditions.alwaysTrue());
    String actualDescriptorsMsg = stringifyDescriptors(descriptors);
    Assert.assertEquals(actualDescriptorsMsg, 1, descriptors.size());
    RunContentDescriptor descriptor = ContainerUtil.getFirstItem(descriptors);
    return ObjectUtils.notNull(descriptor);
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
  private static String stringifyDescriptors(@NotNull List<RunContentDescriptor> descriptors) {
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
