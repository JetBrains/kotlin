/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis;

import com.intellij.analysis.dialog.ModelScopeItem;

import javax.swing.*;
import java.util.List;

public final class ModelScopeItemView {
  public final JRadioButton button;
  public final List<JComponent> additionalComponents;
  public final ModelScopeItem model;
  @AnalysisScope.Type
  public final int scopeId;

  public ModelScopeItemView(JRadioButton button, List<JComponent> components, ModelScopeItem model, int id) {
    this.button = button;
    additionalComponents = components;
    this.model = model;
    scopeId = id;
  }
}
