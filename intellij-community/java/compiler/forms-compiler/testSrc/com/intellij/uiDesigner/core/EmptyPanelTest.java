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
import java.util.Arrays;

public final class EmptyPanelTest extends TestCase {

  public void test1() {
    final GridLayoutManager layoutManager = new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), 0, 0);
    final JPanel panel = new JPanel(layoutManager);

    assertEquals(0, panel.getPreferredSize().width);
    assertEquals(0, panel.getPreferredSize().height);

    panel.setSize(90, 200);
    panel.doLayout(); // should not crash with exception

    assertTrue(Arrays.equals(new int[]{0, 30, 60}, layoutManager.getXs()));
    assertTrue(Arrays.equals(new int[]{30, 30, 30}, layoutManager.getWidths()));

    assertTrue(Arrays.equals(new int[]{0, 100}, layoutManager.getYs()));
    assertTrue(Arrays.equals(new int[]{100, 100}, layoutManager.getHeights()));

    // add component 
    final JButton button = new JButton();
    SpansTest.setDefaults(button);
    button.setPreferredSize(new Dimension(100, 40));
    panel.add(button,
              new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW,
                                  GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    // wisdom
    layoutManager.invalidateLayout(panel);

    assertEquals(new Dimension(100, 40), panel.getPreferredSize());
    panel.setSize(panel.getPreferredSize());
    panel.doLayout();
    assertEquals(100, button.getWidth());
    assertEquals(40, panel.getHeight());

    assertTrue(Arrays.equals(new int[]{0, 40}, layoutManager.getYs()));
    assertTrue(Arrays.equals(new int[]{40, 0}, layoutManager.getHeights()));

    assertTrue(Arrays.equals(new int[]{0, 0, 100}, layoutManager.getXs()));
    assertTrue(Arrays.equals(new int[]{0, 100, 0}, layoutManager.getWidths()));
  }
}
