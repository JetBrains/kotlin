// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.items;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RunAnythingActionItem<T extends AnAction> extends RunAnythingItemBase {
  @NotNull private final T myAction;

  public RunAnythingActionItem(@NotNull T action, @NotNull String fullCommand, @Nullable Icon icon) {
    super(fullCommand, icon);
    myAction = action;
  }

  @NotNull
  public static String getCommand(@NotNull AnAction action, @NotNull String command) {
    return command + " " + (action.getTemplatePresentation().getText() != null ? action.getTemplatePresentation().getText() : "undefined");
  }

  @Nullable
  @Override
  public String getDescription() {
    return myAction.getTemplatePresentation().getDescription();
  }
}