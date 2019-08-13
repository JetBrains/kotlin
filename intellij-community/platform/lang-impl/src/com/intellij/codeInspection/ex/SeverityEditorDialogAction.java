// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Dmitry Batkovich
 */
public class SeverityEditorDialogAction extends AnAction implements DumbAware {

  public SeverityEditorDialogAction() {
    super("Show Severities Editor");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      SeverityEditorDialog.show(project, null, SeverityRegistrar.getSeverityRegistrar(project), false, null);
    }
  }
}
