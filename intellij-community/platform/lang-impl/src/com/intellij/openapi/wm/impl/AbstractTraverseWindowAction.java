// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.ide.ActiveWindowsWatcher;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.ComponentUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class AbstractTraverseWindowAction extends AnAction {

  protected void doPerform(@NotNull Function<? super Window, ? extends Window> mapWindow) {
    Window w = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();
    if (!w.isVisible() || ComponentUtil.isMinimized(w) || AppUIUtil.isInFullscreen(w)) return;
    if (!ActiveWindowsWatcher.isTheCurrentWindowOnTheActivatedList(w)) return;
    Window window = mapWindow.fun(w);
    Component recentFocusOwner = window.getMostRecentFocusOwner();

    (recentFocusOwner == null || !recentFocusOwner.isFocusable()? window : recentFocusOwner).requestFocus();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(true);
  }
}
