/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.componentsList.components;

import javax.swing.*;
import java.awt.*;

public class ScrollablePanel extends JPanel implements Scrollable {
  private int myUnitHeight = -1;
  private static final int myUnitWidth = 10;

  public ScrollablePanel() {
  }

  public ScrollablePanel(LayoutManager layout) {
    super(layout);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    final FontMetrics fontMetrics = getFontMetrics(getFont());
    if (myUnitHeight < 0) {
      myUnitHeight = fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
    }
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.HORIZONTAL) {
      return myUnitWidth;
    }
    else {
      return myUnitHeight;
    }
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.HORIZONTAL) {
      return visibleRect.width;
    }
    return visibleRect.height;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }
}
