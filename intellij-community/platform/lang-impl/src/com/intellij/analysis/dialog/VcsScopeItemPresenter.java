/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class VcsScopeItemPresenter implements ModelScopeItemPresenter {

  @Override
  public int getScopeId() {
    return AnalysisScope.UNCOMMITTED_FILES;
  }

  @NotNull
  @Override
  public JRadioButton getButton(ModelScopeItem m) {
    JRadioButton button = new JRadioButton();
    button.setText(AnalysisScopeBundle.message("scope.option.uncommitted.files"));
    return button;
  }

  @NotNull
  @Override
  public List<JComponent> getAdditionalComponents(JRadioButton button, ModelScopeItem m) {
    VcsScopeItem model = (VcsScopeItem)m;
    ComboBox<String> myChangeLists = new ComboBox<>();
    myChangeLists.setRenderer(new ListCellRendererWrapper<String>() {
      @Override
      public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
        int availableWidth = myChangeLists.getWidth(); // todo, is it correct?
        if (availableWidth <= 0) {
          availableWidth = JBUI.scale(200);
        }
        if (list.getFontMetrics(list.getFont()).stringWidth(value) < availableWidth) {
          setText(value);
        }
        else {
          setText(StringUtil.trimLog(value, 50));
        }
      }
    });

    myChangeLists.setModel(model.getChangeListsModel());
    myChangeLists.setEnabled(button.isSelected());
    button.addItemListener(e -> myChangeLists.setEnabled(button.isSelected()));
    ArrayList<JComponent> components = new ArrayList<>();
    components.add(myChangeLists);
    return components;
  }

  @Override
  public boolean isApplicable(ModelScopeItem model) {
    return model instanceof VcsScopeItem;
  }
}