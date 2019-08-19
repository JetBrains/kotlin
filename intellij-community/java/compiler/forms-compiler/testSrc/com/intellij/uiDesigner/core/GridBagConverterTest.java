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

import com.intellij.uiDesigner.compiler.GridBagConverter;
import com.intellij.util.lang.JavaVersion;
import junit.framework.TestCase;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class GridBagConverterTest extends TestCase {
  /**
   * button 1
   * <empty>
   * button 2
   */
  public void testLayout2() {
    final GridBagLayout layoutManager = new GridBagLayout();
    final JPanel panel = new JPanel(layoutManager);

    final JButton button1 = new JButton();
    button1.setMinimumSize(new Dimension(9, 7));
    button1.setPreferredSize(new Dimension(50, 10));

    final JButton button2 = new JButton();
    button2.setMinimumSize(new Dimension(15, 6));
    button2.setPreferredSize(new Dimension(50, 10));

    GridBagConverter converter = new GridBagConverter();
    final GridConstraints button1Constraints = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                                                   null, null, null, 0);
    converter.addComponent(button1, button1Constraints);

    final GridConstraints button2Constraints = new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                                                   GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0);

    converter.addComponent(button2, button2Constraints);

    applyConversionResults(panel, converter);

    assertEquals(20, panel.getPreferredSize().height);
    assertEquals(50, panel.getPreferredSize().width);

    assertEquals(17, panel.getMinimumSize().height);
    assertEquals(50, panel.getMinimumSize().width);

    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(50, button1.getHeight());
    assertEquals(50, button2.getHeight());
  }

  public void testLayout2ByConstraints() {
    final GridBagLayout layoutManager = new GridBagLayout();
    final JPanel panel = new JPanel(layoutManager);
    final JButton button1 = new JButton();
    final JButton button2 = new JButton();

    GridBagConverter converter = new GridBagConverter();
    final GridConstraints button1Constraints = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                                                   new Dimension(9, 7), new Dimension(50, 10), null, 0);
    converter.addComponent(button1, button1Constraints);

    final GridConstraints button2Constraints = new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                                                   GridConstraints.SIZEPOLICY_FIXED,
                                                                   new Dimension(15, 6), new Dimension(50, 10), null, 0);

    converter.addComponent(button2, button2Constraints);

    applyConversionResults(panel, converter);

    assertEquals(20, panel.getPreferredSize().height);
    assertEquals(50, panel.getPreferredSize().width);

    assertEquals(17, panel.getMinimumSize().height);
    assertEquals(50, panel.getMinimumSize().width);

    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(50, button1.getHeight());
    assertEquals(50, button2.getHeight());
  }

  public void testLayout3() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton button1 = new JButton();
    button1.setPreferredSize(new Dimension(100,20));
    final JButton button2 = new JButton();
    button2.setPreferredSize(new Dimension(100,100));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(button1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(button2, new GridConstraints(1,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);
    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(120, preferredSize.height);
  }

  public void testLayout4() {
    final JPanel panel = new JPanel(new GridBagLayout());

    // button 1  button 3
    // button 2  button 3

    final JButton button1 = new JButton();
    button1.setPreferredSize(new Dimension(100,10));
    final JButton button2 = new JButton();
    button2.setPreferredSize(new Dimension(100,10));
    final JButton button3 = new JButton();
    button3.setPreferredSize(new Dimension(100,200));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(button1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW + GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null,
      0));

    converter.addComponent(button2, new GridConstraints(1,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW + GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null,
      0));

    converter.addComponent(button3, new GridConstraints(0,1,2,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW + GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null,
      0));

    applyConversionResults(panel, converter);
    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(200, preferredSize.height);
  }

  /* TODO[yole]: this layout does not work as expected at runtime
  public void testLayout5_1() {
    final JPanel panel = new JPanel(new GridBagLayout());

    // label textfield(span 2)
    // textfield(span 2)

    final JTextField label = new JTextField();
    label.setPreferredSize(new Dimension(10,30));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,30));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(100,30));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(label, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null));

    converter.addComponent(field1, new GridConstraints(0,1,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null));

    converter.addComponent(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null));

    applyConversionResults(panel, converter);
    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(110, preferredSize.width);
    assertEquals(60, preferredSize.height);
  }
  */

  public void testLayout7() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JLabel label = new JLabel();
    label.setPreferredSize(new Dimension(50,10));

    final JTextField field = new JTextField();
    field.setPreferredSize(new Dimension(100,10));

    final JTextField scroll = new JTextField();
    scroll.setPreferredSize(new Dimension(503, 10));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(label, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    converter.addComponent(field, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    converter.addComponent(scroll, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(0,0), null, null, 0));

    applyConversionResults(panel, converter);

    assertEquals(503, panel.getMinimumSize().width);
    assertEquals(503, panel.getPreferredSize().width);

    panel.setSize(503, 100);
    panel.doLayout();

    assertEquals(50, label.getWidth());
    assertEquals(453, field.getWidth());
  }

  public void testLayout8() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JLabel label1 = new JLabel();
    label1.setMinimumSize(new Dimension(10,10));
    label1.setPreferredSize(new Dimension(100,10));

    final JLabel label2 = new JLabel();
    label2.setMinimumSize(new Dimension(10,10));
    label2.setPreferredSize(new Dimension(100,10));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(label1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK + GridConstraints.SIZEPOLICY_CAN_GROW,
      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(label2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK,
      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(new JLabel(), new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK + GridConstraints.SIZEPOLICY_CAN_GROW + GridConstraints.SIZEPOLICY_WANT_GROW,
      GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0));
    applyConversionResults(panel, converter);

    assertEquals(20, panel.getMinimumSize().width);
    assertEquals(200, panel.getPreferredSize().width);

    // minimum
    panel.setSize(20, 100);
    panel.doLayout();
    assertEquals(10, label1.getWidth());
    assertEquals(10, label2.getWidth());

    // between min and pref
    /* TODO[yole]: GridBag honors weights in this situation, and GridLayout distributes evenly
    panel.setSize(76, 100);
    panel.doLayout();
    assertEquals(38, label1.getWidth());
    assertEquals(38, label2.getWidth());
    */

    // pref-1
    /* TODO[yole]: investigate
    panel.setSize(199, 100);
    panel.doLayout();
    assertEquals(100, label1.getWidth());
    assertEquals(99, label2.getWidth());
    */

    // pref
    panel.setSize(200, 100);
    panel.doLayout();
    assertEquals(100, label1.getWidth());
    assertEquals(100, label2.getWidth());

    // pref+1
    panel.setSize(201, 100);
    panel.doLayout();
    assertEquals(101, label1.getWidth());
    assertEquals(100, label2.getWidth());

    // pref + few
    panel.setSize(205, 100);
    panel.doLayout();
    assertEquals(105, label1.getWidth());
    assertEquals(100, label2.getWidth());
  }

  public void testPrefSize1() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(110,10));
    field1.setPreferredSize(new Dimension(120,10));

    final JTextField field2 = new JTextField();
    field2.setMinimumSize(new Dimension(215,10));
    field2.setPreferredSize(new Dimension(225,10));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

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

  /* TODO[yole]: this relies on strange myMinCellSize logic
  public void testPrefSize2() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,10));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(200,10));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    converter.addComponent(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(300 + 20, preferredSize.width);

    panel.setSize(preferredSize.width, preferredSize.height);
    panel.doLayout();

    assertEquals(100, field1.getWidth());
    assertEquals(200, field2.getWidth());

    panel.setSize(270, preferredSize.height);
    panel.doLayout();   // should not fail
  }
  */

  /**
   * button(can grow) | text field (want grow)
   *   text field (want grow, span 2)
   */
  public void testSpans1() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(50, 10));

    final JTextField field2 = new JTextField();

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(button, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field1, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(100, preferredSize.width);

    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(500, field2.getWidth());
    assertEquals(50, button.getWidth());
    assertEquals(450, field1.getWidth());
  }

  /**
   * button(can grow) | text field (can grow)
   *   text field (want grow, span 2)
   */
  public void testSpans2() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(50, 10));

    final JTextField field2 = new JTextField();

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(button, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field1, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
                                          GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(100, preferredSize.width);

    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(500, field2.getWidth());
    assertEquals(250, button.getWidth());
    assertEquals(250, field1.getWidth());
  }

  /**
   * button(can grow) | text field (want grow, span 2)
   */
  public void testSpans3() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(110, 10));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(button, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field1, new GridConstraints(0,1,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(160, preferredSize.width);

    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(50, button.getWidth());
    assertEquals(450, field1.getWidth());
  }

  /**
   * button (can grow, span 2 )       | text field 1 (span 1)
   * text field 2 (want grow, span 2) | empty
   */
  public void testSpans4() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton button = new JButton();
    button.setPreferredSize(new Dimension(50, 10));

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(110, 10));

    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(110, 10));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(button, new GridConstraints(0,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field1, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(220, preferredSize.width);

    panel.setSize(new Dimension(500, 100));
    panel.doLayout();

    assertEquals(250, button.getWidth());
    assertEquals(250, field1.getWidth());
    assertEquals(250, field2.getWidth());
  }

  /**
   * label   |    label
   * text area (span 2)
   */
  public void testTextAreas1() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JLabel label1 = new JLabel();
    label1.setPreferredSize(new Dimension(15,20));
    final JLabel label2 = new JLabel();
    label2.setPreferredSize(new Dimension(15,20));
    final JTextArea textArea = new JTextArea();
    textArea.setLineWrap(true);

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(label1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(label2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(textArea, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0));

    applyConversionResults(panel, converter);

    int textAreaWidth = JavaVersion.current().feature >= 9 ? 101 : 100;

    assertEquals(textAreaWidth, textArea.getPreferredSize().width);

    final Dimension initialPreferredSize = panel.getPreferredSize();
    assertEquals(new Dimension(textAreaWidth, 20 + textArea.getPreferredSize().height), initialPreferredSize);

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
  public void testTextAreas2() {
    final JPanel panel = new JPanel(/*new GridLayoutManager(2,2, new Insets(0,0,0,0), 11, 0)*/ new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(15,20));
    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(15,20));
    final JTextField field3 = new JTextField();
    field3.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter();
    converter.addComponent(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field3, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    assertEquals(100, panel.getPreferredSize().width);
  }

  public void testGaps1() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,20));
    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 10, 0, false, false);
    converter.addComponent(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(210, preferredSize.width);
  }

  /**
   * field (span 2) | field (span 1)
   */
  public void testGaps2() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setPreferredSize(new Dimension(100,20));
    final JTextField field2 = new JTextField();
    field2.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 7, 0, false, false);
    converter.addComponent(field1, new GridConstraints(0,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(207, preferredSize.width);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout();

    assertEquals(new Rectangle(0,0,100,20), field1.getBounds());
    assertEquals(new Rectangle(107,0,100,20), field2.getBounds());
  }

  /**
   *
   * btn1   |    btn2  | btn4
   *  btn3 (span 2)    |
   */
  public void testGaps3() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton btn1 = new JButton();
    btn1.setPreferredSize(new Dimension(100,20));
    final JButton btn2 = new JButton();
    btn2.setPreferredSize(new Dimension(100,20));
    final JButton btn3 = new JButton();
    btn3.setPreferredSize(new Dimension(100,20));
    final JButton btn4 = new JButton();
    btn4.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 7, 0, false, false);
    converter.addComponent(btn1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn3, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn4, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(314, preferredSize.width);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout();
  }

  /**
   *
   * btn1   |    btn2  | btn4
   *  btn3 (span 2)    |
   */
  public void testGaps3a() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton btn1 = new JButton();
    btn1.setPreferredSize(new Dimension(100,20));
    final JButton btn2 = new JButton();
    btn2.setPreferredSize(new Dimension(100,20));
    final JButton btn3 = new JButton();
    btn3.setPreferredSize(new Dimension(100,20));
    final JButton btn4 = new JButton();
    btn4.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 1000, 0, false, false);
    converter.addComponent(btn1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn3, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn4, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(2300, preferredSize.width);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout();
  }

  /**
   *
   * btn1   |    btn2
   *  btn3 (span 2)
   */
  public void testGaps3b() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton btn1 = new JButton();
    btn1.setPreferredSize(new Dimension(100,20));
    final JButton btn2 = new JButton();
    btn2.setPreferredSize(new Dimension(100,20));
    final JButton btn3 = new JButton();
    btn3.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 1000, 0, false, false);
    converter.addComponent(btn1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn3, new GridConstraints(1,0,1,2,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(1200, preferredSize.width);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout();
  }

  /**
   * btn1
   * -----
   * empty
   * ----
   * btn2
   */
  public void testGaps4() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton btn1 = new JButton();
    btn1.setPreferredSize(new Dimension(100,20));
    final JButton btn2 = new JButton();
    btn2.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 0, 7, false, false);
    converter.addComponent(btn1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn2, new GridConstraints(2,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(47, preferredSize.height);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout();
  }

  // skipped GapsTest.test5 because its only difference from test4 is spacer usage

  public void testGaps6() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JButton btn1 = new JButton();
    btn1.setPreferredSize(new Dimension(100,20));
    final JButton btn2 = new JButton();
    btn2.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 0, 500, false, false);
    converter.addComponent(btn1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(btn2, new GridConstraints(1,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_HORIZONTAL,
      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

    final Dimension preferredSize = panel.getPreferredSize();
    assertEquals(540, preferredSize.height);

    panel.setSize(panel.getPreferredSize());
    panel.doLayout(); // should not crash
  }

  public void testEqualSizeCells1() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(5,20));
    field1.setPreferredSize(new Dimension(10,20));

    final JTextField field2 = new JTextField();
    field2.setMinimumSize(new Dimension(25,20));
    field2.setPreferredSize(new Dimension(50,20));

    final JTextField field3 = new JTextField();
    field3.setMinimumSize(new Dimension(70,20));
    field3.setPreferredSize(new Dimension(100,20));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 7, 0, true, false);
    converter.addComponent(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(0,1,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    converter.addComponent(field3, new GridConstraints(0,2,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0));

    applyConversionResults(panel, converter);

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

    assertEquals(329, field1.getWidth(), 1.0);
    assertEquals(329, field2.getWidth(), 1.0);
    assertEquals(328, field3.getWidth(), 1.0);
  }

  public void testEqualSizeCells2() {
    final JPanel panel = new JPanel(new GridBagLayout());

    final JTextField field1 = new JTextField();
    field1.setMinimumSize(new Dimension(20, 5));
    field1.setPreferredSize(new Dimension(20, 10));

    final JTextField field2 = new JTextField();
    field2.setMinimumSize(new Dimension(20, 25));
    field2.setPreferredSize(new Dimension(20, 50));

    final JTextField field3 = new JTextField();
    field3.setMinimumSize(new Dimension(20, 70));
    field3.setPreferredSize(new Dimension(20, 100));

    GridBagConverter converter = new GridBagConverter(new Insets(0, 0, 0, 0), 0, 7, false, true);
    converter.addComponent(field1, new GridConstraints(0,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    converter.addComponent(field2, new GridConstraints(1,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    converter.addComponent(field3, new GridConstraints(2,0,1,1,GridConstraints.ANCHOR_CENTER,GridConstraints.FILL_BOTH,
      GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, null, null, null, 0));

    applyConversionResults(panel, converter);

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

  private static void applyConversionResults(final JPanel panel, final GridBagConverter converter) {
    GridBagConverter.Result[] results = converter.convert();
    for (GridBagConverter.Result result : results) {
      JComponent component = result.isFillerPanel ? new JPanel() : result.component;
      if (result.minimumSize != null) {
        component.setMinimumSize(result.minimumSize);
      }
      if (result.preferredSize != null) {
        component.setPreferredSize(result.preferredSize);
      }
      panel.add(component, result.constraints);
    }
  }
}
