// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.newclass;

import com.intellij.ide.ui.newItemPopup.NewItemPopupPanel;

import javax.swing.*;
import java.util.Collections;

public class SimpleCreateFileDialogPanel extends NewItemPopupPanel<Object> {

  public SimpleCreateFileDialogPanel() {
    super(Collections.emptyList(), (list, value, index, isSelected, cellHasFocus) -> new JLabel());
    setTemplatesListVisible(false);
  }

  public JTextField getTextField() {
    return myTextField;
  }
}
