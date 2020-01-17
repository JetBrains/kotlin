/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.execution;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Roman.Chernyatchik, oleg
 */
public class ExecutionModes {
  private static final Logger LOG = Logger.getInstance(ExecutionMode.class);

  /**
   * Process will be run in back ground mode
   */
  public static class BackGroundMode extends ExecutionMode {

    public BackGroundMode(final boolean cancelable, @Nullable final String title) {
      super(cancelable, title, null, true, false, null);
    }

    public BackGroundMode(@Nullable final String title) {
      this(true, title);
    }
  }

  /**
   * Process will be run in modal dialog
   */
  public static class ModalProgressMode extends ExecutionMode {

    public ModalProgressMode(final boolean cancelable, @Nullable final String title, JComponent progressParentComponent) {
      super(cancelable, title, null, false, true, progressParentComponent);
    }

    public ModalProgressMode(@Nullable final String title) {
      this(true, title, null);
    }

    public ModalProgressMode(@Nullable final String title, JComponent progressParentComponent) {
      this(true, title, progressParentComponent);
    }
  }

  /**
   * Process will be run in the same thread.
   */
  public static class SameThreadMode extends ExecutionMode {
    private final int myTimeout;

    public SameThreadMode(final boolean cancelable,
                          @Nullable final String title2,
                          final int timeout) {
      super(cancelable, null, title2, false, false, null);
      myTimeout = timeout;
    }

    public SameThreadMode(@Nullable final String title2) {
      this(true, title2, -1);
    }

    /**
     * @param cancelable
     */
    public SameThreadMode(final boolean cancelable) {
      this(cancelable, null, -1);
    }

    /**
     * @param timeout If less than zero it will be ignored
     */
    public SameThreadMode(final int timeout) {
      this(false, null, timeout);
    }

    public SameThreadMode() {
      this(true);
    }

    @Override
    public int getTimeout() {
      return myTimeout;
    }

    @Override
    public void onTimeout(@NotNull ProcessHandler processHandler,
                          @NotNull String commandLineString,
                          @NotNull ProcessOutput outputCollected,
                          @Nullable Throwable invocatorStack) {
      super.onTimeout(processHandler, commandLineString, outputCollected, invocatorStack);
      String output = "\n    stdout: " + outputCollected.getStdout() + ";\n    stderr: " + outputCollected.getStderr();
      LOG.error(new Throwable("Timeout (" + getTimeout() + " sec) on executing: " + commandLineString + "; output collected: " + output,
                              invocatorStack));
    }
  }
}
