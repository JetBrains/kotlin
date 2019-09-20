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

public final class PrefSizeTest extends TestCase{
  /**
   * control(min size 110, pref size 120) control(min size 215, pref size 225)
   */ 
  public void test1() {
    final GridLayoutManager layoutManager = new GridLayoutManager(1,2, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(110,10));
    field1.setPreferredSize(new Dimension(120,10));
    
    final JTextField field2 = new JTextField();
    field2.setMinimumSize(new Dimension(215,10));
    field2.setPreferredSize(new Dimension(225,10));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.doLayout();

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(345, preferredSize.width);

    final Dimension minSize = panel.getMinimumSize();
    assertEquals(325, minSize.width);

    panel.setSize(preferredSize.width, preferredSize.height);
    panel.doLayout();

    assertEquals(120, field1.getWidth());
    assertEquals(225, field2.getWidth());

    panel.setSize(400, panel.getWidth());
    panel.invalidate(); // to invalidate layout
    panel.doLayout();

  }

  public void test2() {
    final GridLayoutManager layoutManager = new GridLayoutManager(1,3); // min cell size should
    layoutManager.setHGap(0);
    layoutManager.setVGap(0);
    final JPanel panel = new JPanel(layoutManager);

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,10));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(200,10));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(300 + 20, preferredSize.width);

    panel.setSize(preferredSize.width, preferredSize.height);
    panel.doLayout();
    
    assertEquals(100, field1.getWidth());
    assertEquals(200, field2.getWidth());

    panel.setSize(270, preferredSize.height);
    panel.doLayout();   // should not fail
  }
  
}
