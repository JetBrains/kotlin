/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ui.DialogUtil;
import com.intellij.util.ui.UpDownHandler;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class ComboBoxVisibilityPanel<V> extends VisibilityPanelBase<V> {
  private final JLabel myLabel;
  protected final JComboBox myComboBox;
  private final Map<V, String> myNamesMap = new HashMap<>();

  public ComboBoxVisibilityPanel(String name, V[] options, String[] presentableNames) {
    setLayout(new BorderLayout(0,2));
    myLabel = new JLabel(name);
    add(myLabel, BorderLayout.NORTH);
    myComboBox = new JComboBox(options);
    myComboBox.setRenderer(getRenderer());
    IJSwingUtilities.adjustComponentsOnMac(myLabel, myComboBox);
    add(myComboBox, BorderLayout.SOUTH);
    for (int i = 0; i < options.length; i++) {
      myNamesMap.put(options[i], presentableNames[i]);
    }
    myComboBox.addActionListener(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stateChanged(new ChangeEvent(ComboBoxVisibilityPanel.this));
      }
    });

    myLabel.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        myComboBox.showPopup();
      }
    });
    DialogUtil.registerMnemonic(myLabel, myComboBox);
  }

  protected ListCellRendererWrapper getRenderer() {
    return new ListCellRendererWrapper() {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        setText(myNamesMap.get(value));
      }
    };
  }

  public ComboBoxVisibilityPanel(String name, V[] options) {
    this(name, options, getObjectNames(options));
  }

  private static String[] getObjectNames(Object[] options) {
    String[] names = new String[options.length];

    for (int i = 0; i < options.length; i ++) {
      names[i] = options[i].toString();
    }

    return names;
  }

  public ComboBoxVisibilityPanel(V[] options) {
    this(RefactoringBundle.message("visibility.combo.title"), options);
  }

  public ComboBoxVisibilityPanel(V[] options, String[] presentableNames) {
    this(RefactoringBundle.message("visibility.combo.title"), options, presentableNames);
  }

  protected void addOption(int index, V option, String presentableName, boolean select) {
    myNamesMap.put(option, presentableName);
    myComboBox.insertItemAt(option, index);

    if (select) {
      myComboBox.setSelectedIndex(index);
    }
  }

  protected void addOption(V option) {
    addOption(myComboBox.getItemCount(), option, option.toString(), false);
  }

  public void setDisplayedMnemonicIndex(int index) {
    myLabel.setDisplayedMnemonicIndex(index);
  }

  @Override
  public V getVisibility() {
    return (V)myComboBox.getSelectedItem();
  }

  public final void registerUpDownActionsFor(JComponent input) {
    UpDownHandler.register(input, myComboBox);
  }

  @Override
  public void setVisibility(V visibility) {
    myComboBox.setSelectedItem(visibility);
    stateChanged(new ChangeEvent(this));
  }
}
