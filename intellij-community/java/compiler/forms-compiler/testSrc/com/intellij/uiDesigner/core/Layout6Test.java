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

public final class Layout6Test extends TestCase{
  

  /**
   * control(min size 10) control(same) control(same)
   */ 
  public void test1() {
    final GridLayoutManager layoutManager = new GridLayoutManager(2,3, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(10,30));
    
    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(10,30));

    final JTextField field3 = new JTextField();
    field3.setPreferredSize(new Dimension(10,30));
    
    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.add(field3, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    panel.doLayout();

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(30, preferredSize.width);
    
    for (int size = 31; size < 100; size++) {
      panel.setSize(size, 30);
      layoutManager.invalidateLayout(panel); // wisdom
      panel.doLayout();
    }
  }

}
