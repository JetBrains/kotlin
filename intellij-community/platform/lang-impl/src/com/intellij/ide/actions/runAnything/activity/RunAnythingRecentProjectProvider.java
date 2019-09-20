// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.ide.actions.runAnything.RunAnythingContext;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RunAnythingRecentProjectProvider extends RunAnythingAnActionProvider<AnAction> {
  @NotNull
  @Override
  public Collection<AnAction> getValues(@NotNull DataContext dataContext, @NotNull String pattern) {
    return Arrays.stream(RecentProjectsManager.getInstance().getRecentProjectsActions(false)).collect(Collectors.toList());
  }

  @NotNull
  @Override
  public RunAnythingItem getMainListItem(@NotNull DataContext dataContext, @NotNull AnAction value) {
    if (value instanceof ReopenProjectAction) {
      return new RecentProjectElement(((ReopenProjectAction)value), getCommand(value), value.getTemplatePresentation().getIcon());
    }
    return super.getMainListItem(dataContext, value);
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

  @Nullable
  @Override
  public String getHelpGroupTitle() {
    return IdeBundle.message("run.anything.recent.project.help.group.title");
  }

  @NotNull
  @Override
  public String getCommand(@NotNull AnAction value) {
    return getHelpCommand() + " " + ObjectUtils
      .notNull(value.getTemplatePresentation().getText(), IdeBundle.message("run.anything.actions.undefined"));
  }

  static class RecentProjectElement extends RunAnythingItemBase {
    @NotNull private final ReopenProjectAction myValue;

    RecentProjectElement(@NotNull ReopenProjectAction value, @NotNull String command, @Nullable Icon icon) {
      super(command, icon);
      myValue = value;
    }

    @Nullable
    @Override
    public String getDescription() {
      return myValue.getProjectPath();
    }
  }

  @NotNull
  @Override
  public List<RunAnythingContext> getExecutionContexts(@NotNull DataContext dataContext) {
    return ContainerUtil.emptyList();
  }
}