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
package com.intellij.application.options.codeStyle.arrangement.util;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.border.Border;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class CalloutBorder implements Border {

  @NotNull private static final Insets INSETS = new Insets(
    ArrangementConstants.CALLOUT_BORDER_HEIGHT,
    ArrangementConstants.HORIZONTAL_PADDING,
    ArrangementConstants.VERTICAL_PADDING,
    ArrangementConstants.HORIZONTAL_PADDING
  );

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Color oldColor = g.getColor();
    g.setColor(JBColor.border());
    
    Graphics2D g2 = (Graphics2D)g;
    Object oldHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    try {
      doPaint(c, g, x, y, width, height);
    }
    finally {
      g.setColor(oldColor);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
    }
  }

  private static void doPaint(Component c, Graphics g, int x, int y, int width, int height) {
    g.drawLine(x + INSETS.left - 1, y + INSETS.top - 1, x + INSETS.left - 1, y + height - INSETS.bottom - 1); // Left border.
    g.drawLine(x + INSETS.left - 1, y + height - INSETS.bottom, x + width - INSETS.right, y + height - INSETS.bottom); // Bottom border.
    g.drawLine(x + width - INSETS.right, y + height - INSETS.bottom - 1, x + width - INSETS.right, y + INSETS.top - 1); // Right border.
    
    int calloutWidth = INSETS.top * 3 / 2;
    if (calloutWidth % 2 != 0) {
      calloutWidth++;
    }
    int hPadding = INSETS.left + INSETS.right;
    
    int leftTopWidth = (width - hPadding - calloutWidth) / 2;
    g.drawLine(x + INSETS.left - 1, y + INSETS.top - 1, x + INSETS.left - 1 + leftTopWidth, y + INSETS.top - 1);
    
    int rightTopWidth = width - leftTopWidth - hPadding - calloutWidth;
    g.drawLine(x + width - rightTopWidth - INSETS.right, y + INSETS.top - 1, x + width - INSETS.right, y + INSETS.top - 1);
    
    g.drawLine(x + INSETS.left - 1 + leftTopWidth, y + INSETS.top - 1, x + INSETS.left - 1 + leftTopWidth + calloutWidth / 2, 0);
    g.drawLine(x + INSETS.left - 1 + leftTopWidth + calloutWidth / 2, 0, x + width - rightTopWidth - INSETS.right, y + INSETS.top - 1);
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return (Insets)INSETS.clone();
  }

  @Override
  public boolean isBorderOpaque() {
    return false;
  }
}
