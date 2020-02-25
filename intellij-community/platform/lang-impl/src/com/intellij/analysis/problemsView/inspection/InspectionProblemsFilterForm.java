// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.inspection;

import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;

public class InspectionProblemsFilterForm {

  interface FilterListener {
    void filtersChanged();

    void filtersResetRequested();
  }


  private JPanel myMainPanel;

  private JBCheckBox myErrorsCheckBox;
  private JBCheckBox myWarningsCheckBox;
  private JBCheckBox myHintsCheckBox;

  private void createUIComponents() {
  }

  public void reset(@NotNull final InspectionProblemsPresentationHelper presentationHelper) {
    myErrorsCheckBox.setSelected(presentationHelper.isShowErrors());
    myWarningsCheckBox.setSelected(presentationHelper.isShowWarnings());
    myHintsCheckBox.setSelected(presentationHelper.isShowHints());
  }

  public void addListener(@NotNull final FilterListener filterListener) {
    final ActionListener listener = e -> filterListener.filtersChanged();

    myErrorsCheckBox.addActionListener(listener);
    myWarningsCheckBox.addActionListener(listener);
    myHintsCheckBox.addActionListener(listener);
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  public boolean isShowErrors() {
    return myErrorsCheckBox.isSelected();
  }

  public boolean isShowWarnings() {
    return myWarningsCheckBox.isSelected();
  }

  public boolean isShowHints() {
    return myHintsCheckBox.isSelected();
  }

}
