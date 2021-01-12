// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollBar;

import javax.swing.*;
import java.awt.*;

public class LocalInvisibleScrollBar extends JBScrollBar {

  private static final Logger LOG = Logger.getInstance(LocalInvisibleScrollBar.class);

  private final EditorModel myEditorModel;

  public LocalInvisibleScrollBar(EditorModel editorModel) {
    super();
    myEditorModel = editorModel;
    setModel(new MyBoundedRangeModel());

    // to make it "invisible":
    setAllSizesToZero();
  }

  private void setAllSizesToZero() {
    Dimension zeroDimension = new Dimension(0, 0);
    setMinimumSize(zeroDimension);
    setPreferredSize(zeroDimension);
    setMaximumSize(zeroDimension);
  }

  private class MyBoundedRangeModel extends DefaultBoundedRangeModel {

    @Override
    public void setRangeProperties(int newValue, int newExtent, int newMin, int newMax, boolean adjusting) {
      super.setRangeProperties(newValue, newExtent, newMin, newMax, adjusting);
      myEditorModel.fireLocalScrollBarValueChanged();
    }
  }
}