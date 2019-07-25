/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CustomScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.CUSTOM;
  }

  @NotNull
  @Override
  public JRadioButton getButton(ModelScopeItem model) {
    JRadioButton button = new JRadioButton();
    button.setText(AnalysisScopeBundle.message("scope.option.custom"));
    return button;
  }

  @NotNull
  @Override
  public List<JComponent> getAdditionalComponents(JRadioButton button, ModelScopeItem m) {
    CustomScopeItem model = (CustomScopeItem) m;
    ScopeChooserCombo scopeCombo = new ScopeChooserCombo();
    scopeCombo.init(model.getProject(), model.getSearchInLibFlag(), true, model.getPreselectedCustomScope());
    scopeCombo.setCurrentSelection(false);
    scopeCombo.setEnabled(button.isSelected());
    model.setSearchScopeSupplier(() -> scopeCombo.getSelectedScope());
    button.addItemListener(e -> scopeCombo.setEnabled(button.isSelected()));
    ArrayList<JComponent> components = new ArrayList<>();
    components.add(scopeCombo);
    return components;
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof CustomScopeItem;
  }
}