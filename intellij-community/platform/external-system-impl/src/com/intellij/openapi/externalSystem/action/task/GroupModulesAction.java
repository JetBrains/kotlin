// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action.task;

import com.intellij.openapi.externalSystem.action.ExternalSystemViewGearAction;
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl;
import org.jetbrains.annotations.NotNull;

public class GroupModulesAction extends ExternalSystemViewGearAction {
  @Override
  protected boolean isSelected(@NotNull ExternalProjectsViewImpl view) {
    return view.getGroupModules();
  }

  @Override
  protected void setSelected(@NotNull ExternalProjectsViewImpl view, boolean value) {
    view.setGroupModules(value);
  }
}
