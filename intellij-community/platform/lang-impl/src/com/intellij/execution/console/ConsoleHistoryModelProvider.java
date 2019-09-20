// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface ConsoleHistoryModelProvider {
  ExtensionPointName<ConsoleHistoryModelProvider> EP_NAME = ExtensionPointName.create("com.intellij.consoleHistoryModelProvider");

  @Nullable
  ConsoleHistoryModel createModel(@NotNull String persistenceId, @NotNull LanguageConsoleView consoleView);

  static ConsoleHistoryModel findModelForConsole(@NotNull String persistenceId, @NotNull LanguageConsoleView consoleView) {
    for (ConsoleHistoryModelProvider provider : EP_NAME.getExtensionList()) {
      ConsoleHistoryModel model = provider.createModel(persistenceId, consoleView);
      if (model != null) {
        return model;
      }
    }
    return DefaultConsoleHistoryModel.createModel(persistenceId);
  }
}
