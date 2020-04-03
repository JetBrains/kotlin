// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.filters;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class TextConsoleBuilderFactoryImpl extends TextConsoleBuilderFactory {
  @NotNull
  @Override
  public TextConsoleBuilder createBuilder(@NotNull final Project project) {
    return new TextConsoleBuilderImpl(project);
  }

  @NotNull
  @Override
  public TextConsoleBuilder createBuilder(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    return new TextConsoleBuilderImpl(project, scope);
  }
}