/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class OtherScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.FILE;
  }

  @NotNull
  @Override
  public JRadioButton getButton(ModelScopeItem m) {
    OtherScopeItem model = (OtherScopeItem)m;
    AnalysisScope scope = model.getScope();
    JRadioButton button = new JRadioButton();
    String name = scope.getShortenName();
    button.setText(name);
    button.setMnemonic(name.charAt(getSelectedScopeMnemonic(name)));
    return button;
  }

  @NotNull
  @Override
  public List<JComponent> getAdditionalComponents(JRadioButton b, ModelScopeItem m) {
    return Collections.emptyList();
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof OtherScopeItem;
  }

  private static int getSelectedScopeMnemonic(String name) {

    final int fileIdx = StringUtil.indexOfIgnoreCase(name, "file", 0);
    if (fileIdx > -1) {
      return fileIdx;
    }

    final int dirIdx = StringUtil.indexOfIgnoreCase(name, "directory", 0);
    if (dirIdx > -1) {
      return dirIdx;
    }

    return 0;
  }
}