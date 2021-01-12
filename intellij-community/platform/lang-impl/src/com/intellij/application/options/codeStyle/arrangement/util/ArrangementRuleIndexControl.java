// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.util;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class ArrangementRuleIndexControl extends JPanel {

  private boolean myIsError;
  private final int myDiameter;
  private final int myHeight;

  private int    myIndex;
  private char[] myChars;
  private int    myIndexWidth;
  private int    myBaseLine;

  public ArrangementRuleIndexControl(int diameter, int height) {
    myDiameter = diameter;
    myHeight = height;
    setOpaque(true);
  }

  public void setIndex(int index) {
    if (index == myIndex) {
      return;
    }
    myIndex = index;
    String s = String.valueOf(index);
    myChars = s.toCharArray();
    myIndexWidth = getFontMetrics(getFont()).charsWidth(myChars, 0, myChars.length);
    setPreferredSize(new Dimension(myDiameter, myDiameter));
    invalidate();
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getPreferredSize() {
    // We use increment here because the circle is drawn using antialiasing, hence, couple of sibling pixels are used.
    // the border doesn't fit the control bounds without the increment then.
    return new Dimension(myDiameter + 2, myDiameter + 2);
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (myChars == null) {
      return;
    }

    g.setColor(myIsError ? JBColor.red : JBColor.border());
    Rectangle bounds = getBounds();
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int y = Math.max(0, myBaseLine - myHeight - (myDiameter - myHeight) / 2);
    g.drawOval(0, y, myDiameter, myDiameter);

    if (StartupUiUtil.isUnderDarcula()) {
      g.setColor(UIUtil.getLabelForeground());
    }
    g.drawChars(myChars, 0, myChars.length, (bounds.width - myIndexWidth) / 2, myBaseLine);
  }

  public void setError(boolean isError) {
    myIsError = isError;
  }

  public void setBaseLine(int baseLine) {
    myBaseLine = baseLine;
  }
}
