/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class ProjectScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.PROJECT;
  }

  @NotNull
  @Override
  public JRadioButton getButton(ModelScopeItem m) {
    String message = AnalysisScopeBundle.message("scope.option.whole.project");
    JRadioButton button = new JRadioButton();
    button.setText(message);
    return button;
  }

  @NotNull
  @Override
  public List<JComponent> getAdditionalComponents(JRadioButton button, ModelScopeItem model) {
    return Collections.emptyList();
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof ProjectScopeItem;
  }
}