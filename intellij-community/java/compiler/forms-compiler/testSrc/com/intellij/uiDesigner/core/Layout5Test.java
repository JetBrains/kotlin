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
package com.intellij.uiDesigner.core;

import junit.framework.TestCase;

import javax.swing.*;
import java.awt.*;

public final class Layout5Test extends TestCase{
  
  
  public void test1() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,3, new Insets(0,0,0,0), 0, 0));

    // label textfield(span 2)
    // textfield(span 2)
    
    final JTextField label = new JTextField();
    label.setPreferredSize(new Dimension(10,30));
    
    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,30));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(100,30));
    
    panel.add(label, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(field1, new GridConstraints(0,1,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.doLayout();

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(110, preferredSize.width);
    assertEquals(60, preferredSize.height);
  }


  public void test2() {
    final GridLayoutManager layoutManager = new GridLayoutManager(2,3, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    // empty textfield(span 2)
    // textfield(span 2) empty

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,30));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(100,30));

    panel.add(field1, new GridConstraints(0,1,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.doLayout();

    final Dimension preferredSize = panel.getPreferredSize();

    // after getPreferredSize() invocation, the field should be not null
    final DimensionInfo horizontalInfo = layoutManager.myHorizontalInfo;
    assertEquals(3, horizontalInfo.getCellCount());
    assertEquals(GridConstraints.SIZEPOLICY_CAN_SHRINK, horizontalInfo.getCellSizePolicy(0));
    assertEquals(GridConstraints.SIZEPOLICY_WANT_GROW, horizontalInfo.getCellSizePolicy(1));
    assertEquals(GridConstraints.SIZEPOLICY_CAN_SHRINK, horizontalInfo.getCellSizePolicy(2));

    assertEquals(100, preferredSize.width);
    assertEquals(60, preferredSize.height);

    panel.setSize(400, 100);
    panel.doLayout();
    assertEquals(400, field1.getWidth());
    assertEquals(400, field2.getWidth());
  }

  public void test3() {
    final GridLayoutManager layoutManager = new GridLayoutManager(2,3, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    // label textfield(span 2)
    // textfield(span 2) label

    final JTextField label1 = new JTextField();
    label1.setPreferredSize(new Dimension(10,30));

    final JTextField label2 = new JTextField();
    label2.setPreferredSize(new Dimension(10,30));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,30));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(100,30));

    panel.add(field1, new GridConstraints(0,1,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(label1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(label2, new GridConstraints(1,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.doLayout();

    final Dimension preferredSize = panel.getPreferredSize();

    // after getPreferredSize() invocation, the field should be not null 
    final DimensionInfo horizontalInfo = layoutManager.myHorizontalInfo;
    assertEquals(GridConstraints.SIZEPOLICY_FIXED, horizontalInfo.getCellSizePolicy(0));
    assertEquals(GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, horizontalInfo.getCellSizePolicy(1));
    assertEquals(GridConstraints.SIZEPOLICY_FIXED, horizontalInfo.getCellSizePolicy(2));
    
    assertEquals(110, preferredSize.width);
    assertEquals(60, preferredSize.height);
    
    panel.setSize(400, 100);
    panel.doLayout();
    assertEquals(390, field1.getWidth());
    assertEquals(390, field2.getWidth());
    assertEquals(10, label1.getWidth());
    assertEquals(10, label2.getWidth());
  }
}
