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

/**
 * @author yole
 */
public class IndentTest extends TestCase {
  public void testSimple() {
    final GridLayoutManager layout = new GridLayoutManager(1,1, new Insets(0,0,0,0), 0, 0);
    final JPanel panel = new JPanel(layout);

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(5,20));
    field1.setPreferredSize(new Dimension(10,20));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1));
    panel.doLayout();

    assertEquals(15, panel.getMinimumSize().width);
    assertEquals(20, panel.getPreferredSize().width);

    panel.setSize(new Dimension(100, 100));
    panel.doLayout();

    assertEquals(10, field1.getBounds().x);
    assertEquals(90, field1.getBounds().width);
  }
}
