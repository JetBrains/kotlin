/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.ModelScopeItemView;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;


public interface ModelScopeItemPresenter {
  ExtensionPointName<ModelScopeItemPresenter> EP_NAME = ExtensionPointName.create("com.intellij.modelScopeItemPresenter");

  @AnalysisScope.Type
  int getScopeId();

  @NotNull
  JRadioButton getButton(ModelScopeItem model);

  @NotNull
  List<JComponent> getAdditionalComponents(JRadioButton button, ModelScopeItem model);

  boolean isApplicable(ModelScopeItem model);

  @NotNull
  static List<ModelScopeItemView> createOrderedViews(List<? extends ModelScopeItem> models) {
    List<ModelScopeItemView> result = new ArrayList<>();
    for (ModelScopeItemPresenter presenter : EP_NAME.getExtensions()) {
      for (ModelScopeItem model : models) {
        if (presenter.isApplicable(model)) {
          JRadioButton button = presenter.getButton(model);
          List<JComponent> components = presenter.getAdditionalComponents(button, model);
          int id = presenter.getScopeId();
          result.add(new ModelScopeItemView(button, components, model, id));
          break;
        }
      }
    }
    return result;
  }
}