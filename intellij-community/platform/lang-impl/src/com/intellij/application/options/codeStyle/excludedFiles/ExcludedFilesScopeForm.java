// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.ui.components.JBList;
import com.intellij.ui.scale.JBUIScale;

import javax.swing.*;
import java.awt.*;

public class ExcludedFilesScopeForm {
  private JPanel myTopPanel;
  private JBList<String> myScopesList;

  public ExcludedFilesScopeForm() {
    myTopPanel.setMinimumSize(new Dimension(myTopPanel.getMinimumSize().width, JBUIScale.scale(100)));
  }

  public JPanel getTopPanel() {
    return myTopPanel;
  }


  public JBList<String> getScopesList() {
    return myScopesList;
  }
}
