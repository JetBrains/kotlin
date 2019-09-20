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
package com.intellij.uiDesigner.compiler;

import com.intellij.uiDesigner.core.AbstractLayout;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Util;
import com.intellij.uiDesigner.lw.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author yole
 */
public class GridBagConverter {
  private final Insets myInsets;
  private int myHGap;
  private int myVGap;
  private boolean mySameSizeHorz;
  private boolean mySameSizeVert;
  private final ArrayList<JComponent> myComponents = new ArrayList<JComponent>();
  private final ArrayList<GridConstraints> myConstraints = new ArrayList<GridConstraints>();
  private int myLastRow = -1;
  private int myLastCol = -1;

  public GridBagConverter() {
    myInsets = new Insets(0, 0, 0, 0);
  }

  public GridBagConverter(final Insets insets, final int hgap, final int vgap, final boolean sameSizeHorz, final boolean sameSizeVert) {
    myInsets = insets;
    myHGap = hgap;
    myVGap = vgap;
    mySameSizeHorz = sameSizeHorz;
    mySameSizeVert = sameSizeVert;
  }

  public void addComponent(final JComponent component, final GridConstraints constraints) {
    myComponents.add(component);
    myConstraints.add(constraints);
  }

  public static void prepareConstraints(final LwContainer container, final Map idToConstraintsMap) {
    GridLayoutManager gridLayout = (GridLayoutManager) container.getLayout();
    GridBagConverter converter = new GridBagConverter(gridLayout.getMargin(),
                                                      getGap(container, true),
                                                      getGap(container, false),
                                                      gridLayout.isSameSizeHorizontally(),
                                                      gridLayout.isSameSizeVertically());
    for(int i=0; i<container.getComponentCount(); i++) {
      final LwComponent component = (LwComponent)container.getComponent(i);
      if (component instanceof LwHSpacer || component instanceof LwVSpacer) {
        GridConstraints constraints = component.getConstraints().store();
        constraints.setHSizePolicy(constraints.getHSizePolicy() & ~GridConstraints.SIZEPOLICY_WANT_GROW);
        constraints.setVSizePolicy(constraints.getVSizePolicy() & ~GridConstraints.SIZEPOLICY_WANT_GROW);
        converter.addComponent(null, constraints);
      }
      else {
        converter.addComponent(null, component.getConstraints());
      }
    }
    Result[] results = converter.convert();
    int componentIndex = 0;
    for (Result result : results) {
      if (!result.isFillerPanel) {
        final LwComponent component = (LwComponent)container.getComponent(componentIndex++);
        idToConstraintsMap.put(component.getId(), result);
      }
      // else generateFillerPanel(generator, componentLocal, results [i]);
    }
  }

  private static int getGap(LwContainer container, final boolean horizontal) {
    while(container != null) {
      final LayoutManager layout = container.getLayout();
      if (layout instanceof AbstractLayout) {
        AbstractLayout aLayout = (AbstractLayout) layout;
        final int gap = horizontal ? aLayout.getHGap() : aLayout.getVGap();
        if (gap >= 0) {
          return gap;
        }
      }
      container = container.getParent();
    }
    return horizontal ? AbstractLayout.DEFAULT_HGAP : AbstractLayout.DEFAULT_VGAP;
  }

  public static class Result {
    public JComponent component;
    public boolean isFillerPanel;
    public GridBagConstraints constraints;
    public Dimension preferredSize;
    public Dimension minimumSize;
    public Dimension maximumSize;

    public Result(final JComponent component) {
      this.component = component;
      constraints = new GridBagConstraints();
    }
  }

  public Result[] convert() {
    ArrayList<Result> results = new ArrayList<Result>();
    for(int i=0; i<myComponents.size(); i++) {
      results.add(convert(myComponents.get(i), myConstraints.get(i)));
    }
    //addFillerPanels(results);
    final Result[] resultArray = results.toArray(new Result[0]);
    if (myHGap > 0 || myVGap > 0) {
      applyGaps(resultArray);
    }
    if (mySameSizeHorz) {
      makeSameSizes(resultArray, true);
    }
    if (mySameSizeVert) {
      makeSameSizes(resultArray, false);
    }

    return resultArray;
  }

  private void applyGaps(final Result[] resultArray) {
    int leftGap = myHGap/2;
    int rightGap = myHGap - myHGap/2;
    int topGap = myVGap / 2;
    int bottomGap = myVGap - myVGap/2;
    for (Result result : resultArray) {
      if (result.constraints.gridx > 0) {
        result.constraints.insets.left += leftGap;
      }
      if (result.constraints.gridx + result.constraints.gridwidth - 1 < myLastCol) {
        result.constraints.insets.right += rightGap;
      }
      if (result.constraints.gridy > 0) {
        result.constraints.insets.top += topGap;
      }
      if (result.constraints.gridy + result.constraints.gridheight - 1 < myLastRow) {
        result.constraints.insets.bottom += bottomGap;
      }
    }
  }

  private static void makeSameSizes(final Result[] resultArray, boolean horizontal) {
    int minimum = -1;
    int preferred = -1;
    for (Result result : resultArray) {
      Dimension minSize = result.minimumSize != null || result.component == null
                          ? result.minimumSize
                          : result.component.getMinimumSize();
      Dimension prefSize = result.preferredSize != null || result.component == null
                           ? result.preferredSize
                           : result.component.getPreferredSize();
      if (minSize != null) {
        minimum = Math.max(minimum, horizontal ? minSize.width : minSize.height);
      }
      if (prefSize != null) {
        preferred = Math.max(preferred, horizontal ? prefSize.width : prefSize.height);
      }
    }

    if (minimum >= 0 || preferred >= 0) {
      for (Result result : resultArray) {
        if ((result.minimumSize != null || result.component != null) && minimum >= 0) {
          if (result.minimumSize == null) {
            result.minimumSize = result.component.getMinimumSize();
          }
          if (horizontal) {
            result.minimumSize.width = minimum;
          }
          else {
            result.minimumSize.height = minimum;
          }
        }

        if ((result.preferredSize != null || result.component != null) && preferred >= 0) {
          if (result.preferredSize == null) {
            result.preferredSize = result.component.getPreferredSize();
          }
          if (horizontal) {
            result.preferredSize.width = preferred;
          }
          else {
            result.preferredSize.height = preferred;
          }
        }
      }
    }
  }

  private Result convert(final JComponent component, final GridConstraints constraints) {
    final Result result = new Result(component);

    int endRow = constraints.getRow() + constraints.getRowSpan()-1;
    myLastRow = Math.max(myLastRow, endRow);
    int endCol = constraints.getColumn() + constraints.getColSpan()-1;
    myLastCol = Math.max(myLastCol, endCol);

    int indent = Util.DEFAULT_INDENT * constraints.getIndent();

    constraintsToGridBag(constraints, result.constraints);
    result.constraints.weightx = getWeight(constraints, true);
    result.constraints.weighty = getWeight(constraints, false);
    result.constraints.insets = new Insets(myInsets.top, myInsets.left + indent, myInsets.bottom, myInsets.right);

    Dimension minSize = constraints.myMinimumSize;
    if (component != null && minSize.width <= 0 && minSize.height <= 0) {
      minSize = component.getMinimumSize();
    }

    if ((constraints.getHSizePolicy() & GridConstraints.SIZEPOLICY_CAN_SHRINK) == 0) {
      minSize.width = constraints.myPreferredSize.width > 0 || component == null
                      ? constraints.myPreferredSize.width
                      : component.getPreferredSize().width;
    }
    if ((constraints.getVSizePolicy() & GridConstraints.SIZEPOLICY_CAN_SHRINK) == 0) {
      minSize.height = constraints.myPreferredSize.height > 0 || component == null
                       ? constraints.myPreferredSize.height
                       : component.getPreferredSize().height;
    }

    if (minSize.width != -1 || minSize.height != -1) {
      result.minimumSize = minSize;
    }

    if (constraints.myPreferredSize.width > 0 && constraints.myPreferredSize.height > 0) {
      result.preferredSize = constraints.myPreferredSize;
    }
    if (constraints.myMaximumSize.width > 0 && constraints.myMaximumSize.height > 0) {
      result.maximumSize = constraints.myMaximumSize;
    }

    return result;
  }

  public static GridBagConstraints getGridBagConstraints(IComponent component) {
    final GridBagConstraints gbc;
    if (component.getCustomLayoutConstraints() instanceof GridBagConstraints) {
      gbc = (GridBagConstraints) component.getCustomLayoutConstraints();
    }
    else {
      gbc = new GridBagConstraints();
    }
    constraintsToGridBag(component.getConstraints(), gbc);
    return gbc;
  }

  public static void constraintsToGridBag(final GridConstraints constraints, final GridBagConstraints result) {
    result.gridx = constraints.getColumn();
    result.gridy = constraints.getRow();
    result.gridwidth = constraints.getColSpan();
    result.gridheight = constraints.getRowSpan();
    switch(constraints.getFill()) {
      case GridConstraints.FILL_HORIZONTAL: result.fill = GridBagConstraints.HORIZONTAL; break;
      case GridConstraints.FILL_VERTICAL:   result.fill = GridBagConstraints.VERTICAL; break;
      case GridConstraints.FILL_BOTH:       result.fill = GridBagConstraints.BOTH; break;
      default:                              result.fill = GridBagConstraints.NONE; break;
    }
    switch(constraints.getAnchor()) {
      case GridConstraints.ANCHOR_NORTHWEST: result.anchor = GridBagConstraints.NORTHWEST; break;
      case GridConstraints.ANCHOR_NORTH:     result.anchor = GridBagConstraints.NORTH; break;
      case GridConstraints.ANCHOR_NORTHEAST: result.anchor = GridBagConstraints.NORTHEAST; break;
      case GridConstraints.ANCHOR_EAST:      result.anchor = GridBagConstraints.EAST; break;
      case GridConstraints.ANCHOR_SOUTHEAST: result.anchor = GridBagConstraints.SOUTHEAST; break;
      case GridConstraints.ANCHOR_SOUTH:     result.anchor = GridBagConstraints.SOUTH; break;
      case GridConstraints.ANCHOR_SOUTHWEST: result.anchor = GridBagConstraints.SOUTHWEST; break;
      case GridConstraints.ANCHOR_WEST:      result.anchor = GridBagConstraints.WEST; break;
    }
  }

  private double getWeight(final GridConstraints constraints, final boolean horizontal) {
    int policy = horizontal ? constraints.getHSizePolicy() : constraints.getVSizePolicy();
    if ((policy & GridConstraints.SIZEPOLICY_WANT_GROW) != 0) {
      return 1.0;
    }
    boolean canGrow = ((policy & GridConstraints.SIZEPOLICY_CAN_GROW) != 0);
    for (Object myConstraint : myConstraints) {
      GridConstraints otherConstraints = (GridConstraints)myConstraint;

      if (!constraintsIntersect(horizontal, constraints, otherConstraints)) {
        int otherPolicy = horizontal ? otherConstraints.getHSizePolicy() : otherConstraints.getVSizePolicy();
        if ((otherPolicy & GridConstraints.SIZEPOLICY_WANT_GROW) != 0) {
          return 0.0;
        }
        if (!canGrow && ((otherPolicy & GridConstraints.SIZEPOLICY_CAN_GROW) != 0)) {
          return 0.0;
        }
      }
    }
    return 1.0;
  }

  private static boolean constraintsIntersect(final boolean horizontal,
                                              final GridConstraints constraints,
                                              final GridConstraints otherConstraints) {
    int start = constraints.getCell(!horizontal);
    int end = start + constraints.getSpan(!horizontal) - 1;
    int otherStart = otherConstraints.getCell(!horizontal);
    int otherEnd = otherStart + otherConstraints.getSpan(!horizontal) - 1;
    return start <= otherEnd && otherStart <= end;
  }
}
