/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution.console;

import org.jetbrains.annotations.NotNull;

public abstract class BaseConsoleExecuteActionHandler extends ConsoleExecuteAction.ConsoleExecuteActionHandler {
  public BaseConsoleExecuteActionHandler(boolean preserveMarkup) {
    super(preserveMarkup);
  }

  @Override
  final void doExecute(@NotNull String text, @NotNull LanguageConsoleView consoleView) {
    execute(text, consoleView);
  }

  protected void execute(@NotNull String text, @NotNull LanguageConsoleView console) {
  }

  public String getEmptyExecuteAction() {
    return ConsoleExecuteAction.CONSOLE_EXECUTE_ACTION_ID;
  }
}