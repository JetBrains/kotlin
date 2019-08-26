// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class AbstractTraverseWindowAction extends AnAction {

  protected void doPerform(@NotNull Function<? super Window, ? extends Window> mapWindow) {
    Window w = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();
    if (!w.isVisible() || UIUtil.isMinimized(w) || AppUIUtil.isInFullscreen(w)) return;
    if (!IdeEventQueue.getInstance().isTheCurrentWindowOnTheActivatedList(w)) return;
    Window window = mapWindow.fun(w);
    Component recentFocusOwner = window.getMostRecentFocusOwner();
    (recentFocusOwner == null ? window : recentFocusOwner).requestFocus();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(true);
  }
}
