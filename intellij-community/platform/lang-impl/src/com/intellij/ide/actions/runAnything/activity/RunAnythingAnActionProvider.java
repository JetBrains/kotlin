// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.RunAnythingUtil;
import com.intellij.ide.actions.runAnything.items.RunAnythingActionItem;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.IdeFocusManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class RunAnythingAnActionProvider<V extends AnAction> extends RunAnythingProviderBase<V> {
  @NotNull
  @Override
  public RunAnythingItem getMainListItem(@NotNull DataContext dataContext, @NotNull V value) {
    return new RunAnythingActionItem<>(value, getCommand(value), value.getTemplatePresentation().getIcon());
  }

  @Override
  public void execute(@NotNull DataContext dataContext, @NotNull V value) {
    performRunAnythingAction(value, dataContext);
  }

  @Nullable
  @Override
  public Icon getIcon(@NotNull V value) {
    return value.getTemplatePresentation().getIcon();
  }

  private static void performRunAnythingAction(@NotNull AnAction action, @NotNull DataContext dataContext) {
    ApplicationManager.getApplication().invokeLater(
      () -> IdeFocusManager.getInstance(RunAnythingUtil.fetchProject(dataContext))
                           .doWhenFocusSettlesDown(() -> performAction(action, dataContext)));
  }

  private static void performAction(@NotNull AnAction action, @NotNull DataContext dataContext) {
    AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext);

    ActionUtil.performActionDumbAwareWithCallbacks(action, event, dataContext);
  }

  @Nullable
  @Override
  public String getAdText() {
    return IdeBundle.message("run.anything.ad.run.action.with.default.settings", RunAnythingUtil.SHIFT_SHORTCUT_TEXT);
  }
}
