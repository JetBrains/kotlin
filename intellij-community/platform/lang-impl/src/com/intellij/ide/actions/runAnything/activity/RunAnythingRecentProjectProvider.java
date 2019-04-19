// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class RunAnythingRecentProjectProvider extends RunAnythingAnActionProvider<AnAction> {
  @NotNull
  @Override
  public Collection<AnAction> getValues(@NotNull DataContext dataContext, @NotNull String pattern) {
    return Arrays.stream(RecentProjectsManager.getInstance().getRecentProjectsActions(false)).collect(Collectors.toList());
  }

  @Override
  @NotNull
  public String getCompletionGroupTitle() {
    return IdeBundle.message("run.anything.recent.project.completion.group.title");
  }

  @NotNull
  @Override
  public String getHelpCommandPlaceholder() {
    return IdeBundle.message("run.anything.recent.project.command.placeholder");
  }

  @NotNull
  @Override
  public String getHelpCommand() {
    return "open";
  }

  @NotNull
  @Override
  public String getCommand(@NotNull AnAction value) {
    return getHelpCommand() + " " + ObjectUtils
      .notNull(value.getTemplatePresentation().getText(), IdeBundle.message("run.anything.actions.undefined"));
  }
}