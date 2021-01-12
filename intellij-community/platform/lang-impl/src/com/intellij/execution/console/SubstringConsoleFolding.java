// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.ConsoleFolding;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class SubstringConsoleFolding extends ConsoleFolding {
  @Override
  public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
    return ConsoleFoldingSettings.getSettings().shouldFoldLine(line);
  }

  @Override
  public String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
    return " <" + lines.size() + " internal " + StringUtil.pluralize("call", lines.size()) + ">";
  }
}
