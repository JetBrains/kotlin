// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.actions.runAnything.RunAnythingCache;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject;

public class RunAnythingRecentCommandProvider extends RunAnythingCommandProvider {

  @NotNull
  @Override
  public Collection<String> getValues(@NotNull DataContext dataContext, @NotNull String pattern) {
    return RunAnythingCache.getInstance(fetchProject(dataContext)).getState().getCommands();
  }
}