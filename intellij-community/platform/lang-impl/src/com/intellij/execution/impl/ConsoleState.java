/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class ConsoleState {
  @NotNull
  public abstract ConsoleState attachTo(@NotNull ConsoleViewImpl console, ProcessHandler processHandler);
  @NotNull
  public abstract ConsoleState dispose();

  public boolean isFinished() {
    return false;
  }

  public boolean isRunning() {
    return false;
  }

  /**
   * @return whether the given line should be folded as a command line if it's first in the console
   */
  public boolean isCommandLine(@NotNull String line) {
    return false;
  }

  public void sendUserInput(@NotNull String input) throws IOException {}

  public abstract static class NotStartedStated extends ConsoleState {
    @NotNull
    @Override
    public ConsoleState dispose() {
      // not disposable
      return this;
    }

    @Override
    public String toString() {
      return "Not started state";
    }
  }
}
