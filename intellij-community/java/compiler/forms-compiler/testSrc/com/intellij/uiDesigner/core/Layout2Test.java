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

public final class Layout2Test extends TestCase{
  /**
   * button 1
   * <empty>
   * button 2 
   */ 
  public void test1() {
    final GridLayoutManager layoutManager = new GridLayoutManager(3,1, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    final JButton button1 = new JButton();
    button1.setMinimumSize(new Dimension(9, 7));
    button1.setPreferredSize(new Dimension(50, 10));

    final JButton button2 = new JButton();
    button2.setMinimumSize(new Dimension(15, 6));
    button2.setPreferredSize(new Dimension(50, 10));
    
    panel.add(button1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    panel.add(button2, new GridConstraints(2,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.doLayout();

    assertEquals(20, panel.getPreferredSize().height);
    assertEquals(50, panel.getPreferredSize().width);
    
    assertEquals(17, panel.getMinimumSize().height);
    assertEquals(50, panel.getMinimumSize().width);
    
    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(50, button1.getHeight());
    assertEquals(50, button2.getHeight());
  }
  
}
