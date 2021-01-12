/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.refactoring.ui;

import com.intellij.refactoring.RefactoringBundle;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author dsl
 */
public class DelegationPanel extends JPanel {
  private final JRadioButton myRbModifyCalls;
  private final JRadioButton myRbGenerateDelegate;

  public DelegationPanel() {
    final BoxLayout boxLayout = new BoxLayout(this, BoxLayout.X_AXIS);
    setLayout(boxLayout);
    add(new JLabel(RefactoringBundle.message("delegation.panel.method.calls.label")));
    add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP)));
    myRbModifyCalls = new JRadioButton();
    myRbModifyCalls.setText(RefactoringBundle.message("delegation.panel.modify.radio"));
    add(myRbModifyCalls);
    add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP)));
    myRbGenerateDelegate = new JRadioButton();
    myRbGenerateDelegate.setText(RefactoringBundle.message("delegation.panel.delegate.via.overloading.method"));
    add(myRbGenerateDelegate);
    add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP)));
    myRbModifyCalls.setSelected(true);
    final ButtonGroup bg = new ButtonGroup();
    bg.add(myRbModifyCalls);
    bg.add(myRbGenerateDelegate);
    add(Box.createHorizontalGlue());
    myRbModifyCalls.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        stateModified();
      }
    });
    myRbGenerateDelegate.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        stateModified();
      }
    });
  }

  protected void stateModified() {

  }

  public boolean isModifyCalls() {
    return myRbModifyCalls.isSelected();
  }

  public boolean isGenerateDelegate() {
    return myRbGenerateDelegate.isSelected();
  }
}
