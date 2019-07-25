// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

public abstract class ToolsCustomizer {
  public static final ExtensionPointName<ToolsCustomizer> EP_NAME = ExtensionPointName.create("com.intellij.toolsCustomizer");

  public static GeneralCommandLine customizeCommandLine(@NotNull GeneralCommandLine commandLine, @NotNull DataContext dataContext) {
    return StreamEx.of(EP_NAME.getExtensions()).foldLeft(commandLine, (context, customizer) ->
      customizer.customizeCommandLine(dataContext, commandLine));
  }

  @NotNull
  public abstract GeneralCommandLine customizeCommandLine(@NotNull DataContext dataContext, @NotNull GeneralCommandLine commandLine);
}