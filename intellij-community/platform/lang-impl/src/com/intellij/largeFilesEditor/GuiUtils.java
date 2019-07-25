// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor;

import com.intellij.ui.JBColor;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBDimension;

import javax.swing.*;

public class GuiUtils {

  private static final int NOT_LIMITED_SIZE = Integer.MAX_VALUE;
  private static final int PANEL_STANDARD_SIZE_HEIGHT = 28;
  private static final int PANEL_STANDARD_SIZE_WIDTH = 32;

  public static void setStandardSizeForPanel(JComponent panel, boolean horizontalPanelOrientation) {
    JBDimension dimension;
    if (horizontalPanelOrientation) {
      dimension = new JBDimension(NOT_LIMITED_SIZE, PANEL_STANDARD_SIZE_HEIGHT);
    }
    else {
      dimension = new JBDimension(PANEL_STANDARD_SIZE_WIDTH, NOT_LIMITED_SIZE);
    }

    panel.setMinimumSize(new JBDimension(0, 0));
    panel.setPreferredSize(dimension);
    panel.setMaximumSize(dimension);
  }

  public static void setStandardLineBorderToPanel(JComponent panel, int top, int left, int bottom, int right) {
    panel.setBorder(new CustomLineBorder(JBColor.border(), top, left, bottom, right));
  }
}
