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

import static com.intellij.uiDesigner.core.SpansTest.setDefaults;

public final class TextAreasTest extends TestCase {
  /**
   * label   |    label
   * text area (span 2)
   */
  public void test1() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,2, new Insets(0,0,0,0), 0, 0));

    final JLabel label1 = new JLabel();
    label1.setPreferredSize(new Dimension(15,20));
    final JLabel label2 = new JLabel();
    label2.setPreferredSize(new Dimension(15,20));
    final JTextArea textArea = new JTextArea();
    textArea.setLineWrap(true);

    panel.add(label1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(label2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(textArea, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0));

    panel.doLayout();

    assertFalse(UIManager.getLookAndFeel().getName().equals("Windows"));
    // This check fails for Windows LaF due to its default TextArea settings, so it's not expected here. By default it's Metal on Windows.
    assertEquals(100, textArea.getPreferredSize().width);

    final Dimension initialPreferredSize = panel.getPreferredSize();
    assertEquals(new Dimension(100,20 + textArea.getPreferredSize().height), initialPreferredSize);

    panel.setSize(initialPreferredSize);
    panel.invalidate();
    panel.doLayout();

    assertEquals(initialPreferredSize, panel.getPreferredSize());
  }

  /**
   * textfield1 | textfield2
   *  textfield3 (span 2)
   *
   * important: hspan should be greater than 0
   */
  public void test2() {
    final JPanel panel = new JPanel(new GridLayoutManager(2,2, new Insets(0,0,0,0), 11, 0));

    final JTextField field1 = new JTextField();
    setDefaults(field1);
    field1.setPreferredSize(new Dimension(15,20));
    final JTextField field2 = new JTextField();
    setDefaults(field2);
    field2.setPreferredSize(new Dimension(15,20));
    final JTextField field3 = new JTextField();
    setDefaults(field3);
    field3.setPreferredSize(new Dimension(100,20));

    panel.add(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.add(field3, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    panel.doLayout();

    assertEquals(100, panel.getPreferredSize().width);
  }

}
