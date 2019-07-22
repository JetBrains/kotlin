// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;


import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

final class PreviousWindow extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Window window = IdeEventQueue.getInstance().nextWindowBefore(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow());
    Component recentFocusOwner = window.getMostRecentFocusOwner();
    (recentFocusOwner == null ? window : recentFocusOwner).requestFocus();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(true);
  }
}
