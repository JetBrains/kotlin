// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface ParameterInfoListener {
  ExtensionPointName<ParameterInfoListener> EP_NAME = new ExtensionPointName<>("com.intellij.codeInsight.parameterInfo.listener");

  /**
   * This method is invoked when parameter info hint content is updated (including first time show) and there are some signatures to show
   * @param result model describing signatures shown and current context
   */
  void hintUpdated(@NotNull ParameterInfoController.Model result);

  /**
   * This method is invoked when parameter info hint is hidden due to closing the hint, leaving parameters range area or because there are
   * no signatures to show
   * @param project for which hint was shown originally
   */
  void hintHidden(@NotNull Project project);
}
