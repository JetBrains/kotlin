// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import com.intellij.ui.components.JBLabel;

import javax.swing.*;

class FileTypePanel {
  JPanel myWholePanel;
  JPanel myRecognizedFileTypesPanel;
  JPanel myPatternsPanel;
  JTextField myIgnoreFilesField;
  JPanel myIgnorePanel;
  JPanel myHashBangPanel;
  JPanel myOpenWithLightEditPanel;
  JTextField myLightEditPatternsField;
  JBLabel myLightEditHintLabel;
}
