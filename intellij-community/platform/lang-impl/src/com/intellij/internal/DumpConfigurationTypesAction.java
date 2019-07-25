// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class DumpConfigurationTypesAction extends AnAction implements DumbAware {
  public DumpConfigurationTypesAction() {
    super("Dump Configurations");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    for (ConfigurationType factory : ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList()) {
      System.out.println(factory.getDisplayName() + " : " + factory.getId());
    }
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}