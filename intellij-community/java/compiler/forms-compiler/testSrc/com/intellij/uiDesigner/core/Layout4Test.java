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

public final class Layout4Test extends TestCase{
  
  
  public void test1() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,2, new Insets(0,0,0,0), 0, 0));

    // button 1  button 3
    // button 2  button 3
    
    final JButton button1 = new JButton();
    button1.setPreferredSize(new Dimension(100,10));
    final JButton button2 = new JButton();
    button2.setPreferredSize(new Dimension(100,10));
    final JButton button3 = new JButton();
    button3.setPreferredSize(new Dimension(100,200));

    panel.add(button1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW + GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null,
      0));

    panel.add(button2, new GridConstraints(1,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW + GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null,
      0));

    panel.add(button3, new GridConstraints(0,1,2,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW + GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null,
      0));

    panel.doLayout();

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(200, preferredSize.height);
  }
}
