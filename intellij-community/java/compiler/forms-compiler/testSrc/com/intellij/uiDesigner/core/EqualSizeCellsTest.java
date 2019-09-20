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

public final class EqualSizeCellsTest extends TestCase{
  /**
   * field1 | field2 | field3
   */ 
  public void test1() {
    final GridLayoutManager layout = new GridLayoutManager(1,3, new Insets(0,0,0,0), 7, 0);

    layout.setSameSizeHorizontally(true);

    final JPanel panel = new JPanel(layout);

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(5,20));
    field1.setPreferredSize(new Dimension(10,20));

    final JTextField field2 = new JTextField();
    field2.setMinimumSize(new Dimension(25,20));
    field2.setPreferredSize(new Dimension(50,20));

    final JTextField field3 = new JTextField();
    field3.setMinimumSize(new Dimension(70,20));
    field3.setPreferredSize(new Dimension(100,20));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field3, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.doLayout();

    final Dimension minimumSize = panel.getMinimumSize();
    assertEquals(70 + 7 + 70 + 7 + 70, minimumSize.width);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(100 + 7 + 100 + 7 + 100, preferredSize.width);

    //
    panel.setSize(panel.getPreferredSize());
    panel.doLayout();

    assertEquals(100, field1.getWidth());
    assertEquals(100, field2.getWidth());
    assertEquals(100, field3.getWidth());

    //
    panel.setSize(new Dimension(1000, 1000));
    panel.doLayout();

    assertEquals(329, field1.getWidth());
    assertEquals(329, field2.getWidth());
    assertEquals(328, field3.getWidth());
  }


  /**
   * field1
   * ------
   * field2
   * ------
   * field3
   */
  public void test2() {
    final GridLayoutManager layout = new GridLayoutManager(3,1, new Insets(0,0,0,0), 0, 7);

    layout.setSameSizeVertically(true);

    final JPanel panel = new JPanel(layout);

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(20, 5));
    field1.setPreferredSize(new Dimension(20, 10));

    final JTextField field2 = new JTextField();
    field2.setMinimumSize(new Dimension(20, 25));
    field2.setPreferredSize(new Dimension(20, 50));

    final JTextField field3 = new JTextField();
    field3.setMinimumSize(new Dimension(20, 70));
    field3.setPreferredSize(new Dimension(20, 100));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    panel.add(field2, new GridConstraints(1,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    panel.add(field3, new GridConstraints(2,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    panel.doLayout();

    final Dimension minimumSize = panel.getMinimumSize();
    assertEquals(70 + 7 + 70 + 7 + 70, minimumSize.height);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(100 + 7 + 100 + 7 + 100, preferredSize.height);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout();

    assertEquals(100, field1.getHeight());
    assertEquals(100, field2.getHeight());
    assertEquals(100, field3.getHeight());
  }
}
