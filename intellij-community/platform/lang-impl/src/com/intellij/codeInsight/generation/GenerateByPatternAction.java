// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.generation;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitry Avdeev
 */
public class GenerateByPatternAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(true);
    for (PatternProvider extension : PatternProvider.EXTENSION_POINT_NAME.getExtensionList()) {
      if (extension.isAvailable(e.getDataContext())) return;
    }
    e.getPresentation().setVisible(false);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PatternDescriptor[] patterns = new PatternDescriptor[0];
    for (PatternProvider extension : PatternProvider.EXTENSION_POINT_NAME.getExtensionList()) {
      if (extension.isAvailable(e.getDataContext())) {
        patterns = ArrayUtil.mergeArrays(patterns, extension.getDescriptors());
      }
    }
    GenerateByPatternDialog dialog = new GenerateByPatternDialog(e.getProject(), patterns);
    if (dialog.showAndGet()) {
      dialog.getSelectedDescriptor().actionPerformed(e.getDataContext());
    }
  }
}
