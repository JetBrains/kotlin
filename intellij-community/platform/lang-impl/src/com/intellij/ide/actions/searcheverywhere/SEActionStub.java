// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class SEActionStub extends DumbAwareAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {}

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
  }

}
