/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.changeSignature;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MultiLineLabelUI;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.refactoring.util.RadioUpDownListener;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DefaultValueChooser extends DialogWrapper{
  private JRadioButton myLeaveBlankRadioButton;
  private JRadioButton myFeelLuckyRadioButton;
  private JLabel myFeelLuckyDescription;
  private JRadioButton myUseValueRadioButton;
  private EditorTextField myValueEditor;
  private JPanel myWholePanel;
  private JLabel myBlankDescription;

  public DefaultValueChooser(Project project, String name, String defaultValue) {
    super(project);
    new RadioUpDownListener(myLeaveBlankRadioButton, myFeelLuckyRadioButton, myUseValueRadioButton);
    final ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myValueEditor.setEnabled(myUseValueRadioButton.isSelected());
        if (myUseValueRadioButton.isSelected()) {
          myValueEditor.selectAll();
          IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myValueEditor, true));
        }
      }
    };
    myLeaveBlankRadioButton.addActionListener(actionListener);
    myFeelLuckyRadioButton.addActionListener(actionListener);
    myUseValueRadioButton.addActionListener(actionListener);
    setTitle("Default value for parameter \"" + name + "\" needed");
    myLeaveBlankRadioButton.setSelected(true);
    myValueEditor.setEnabled(false);
    myFeelLuckyDescription.setText("In calls to this method, if exactly one variable of the correct type is found, use it. Otherwise leave blank.");
    myFeelLuckyDescription.setUI(new MultiLineLabelUI());
    myBlankDescription.setUI(new MultiLineLabelUI());
    myValueEditor.setText(defaultValue);
    init();
  }

  public boolean feelLucky() {
    return myFeelLuckyRadioButton.isSelected();
  }


  public String getDefaultValue() {
    if (myLeaveBlankRadioButton.isSelected()) {
      return "";
    }
    return myValueEditor.getText();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myLeaveBlankRadioButton;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myWholePanel;
  }
}
