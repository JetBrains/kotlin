/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.application.options.colors;

import com.intellij.codeHighlighting.RainbowHighlighter;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.EventDispatcher;
import com.intellij.util.FontUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


public class RainbowDescriptionPanel extends JPanel implements OptionsPanelImpl.ColorDescriptionPanel {
  private final EventDispatcher<Listener> myDispatcher = EventDispatcher.create(Listener.class);

  protected JPanel myPanel;

  private JBLabel myLStop1;
  private JBLabel myLStop2;
  private JBLabel myLStop3;
  private JBLabel myLStop4;
  private JBLabel myLStop5;
  private final JBLabel[] myLStops = new JBLabel[]{myLStop1, myLStop2, myLStop3, myLStop4, myLStop5};

  protected ColorPanel myStop1;
  protected ColorPanel myStop2;
  protected ColorPanel myStop3;
  protected ColorPanel myStop4;
  protected ColorPanel myStop5;
  private final ColorPanel[] myStops = new ColorPanel[]{myStop1, myStop2, myStop3, myStop4, myStop5};

  private JBCheckBox myRainbow;
  private JTextPane myInheritanceLabel;
  private JBCheckBox myInheritAttributesBox;

  public RainbowDescriptionPanel() {
    super(new BorderLayout());
    add(myPanel, BorderLayout.CENTER);

    setBorder(JBUI.Borders.empty(4, 0, 4, 4));

    ActionListener actionListener = e -> myDispatcher.getMulticaster().onSettingsChanged(e);
    for (JBCheckBox c : new JBCheckBox[]{myRainbow, myInheritAttributesBox}) {
      c.addActionListener(actionListener);
    }
    for (ColorPanel c : new ColorPanel[]{myStop1, myStop2, myStop3, myStop4, myStop5}) {
      c.addActionListener(actionListener);
    }

    String languageDefaultPageID = OptionsBundle.message("options.language.defaults.display.name");
    String rainbowOptionsID = ApplicationBundle.message("rainbow.option.panel.display.name");

    // copied from ColorAndFontDescriptionPanel:
    String style = "<div style=\"text-align:right\" vertical-align=\"top\">";
    String inheritanceTooltip = "Editor | Color Scheme | " + languageDefaultPageID + "<br>" + rainbowOptionsID;
    String inheritanceText = style + "<a href=\"" + languageDefaultPageID + "\">" + rainbowOptionsID + "</a><br>" +
                             "(" + languageDefaultPageID + ")";

    Messages.configureMessagePaneUi(myInheritanceLabel, "<html>", null);
    myInheritanceLabel.setText(checkRightArrow(inheritanceText));
    myInheritanceLabel.setToolTipText(checkRightArrow(inheritanceTooltip));
    myInheritanceLabel.addHyperlinkListener(e -> myDispatcher.getMulticaster().onHyperLinkClicked(e));
    myInheritanceLabel.setBorder(JBUI.Borders.empty(4, 0, 4, 4));
  }

  @NotNull
  private static String checkRightArrow(@NotNull String str) {
    return str.replaceAll("->", FontUtil.rightArrow(UIUtil.getLabelFont()));
  }

  @NotNull
  @Override
  public JComponent getPanel() {
    return this;
  }

  @Override
  public void resetDefault() {
  }

  @Override
  public void reset(@NotNull EditorSchemeAttributeDescriptor attributeDescriptor) {
    if (!(attributeDescriptor instanceof RainbowAttributeDescriptor)) return;
    RainbowAttributeDescriptor descriptor = (RainbowAttributeDescriptor)attributeDescriptor;

    EditorColorsScheme editedColorsScheme = descriptor.getScheme();
    Boolean rainbowOn = RainbowHighlighter.isRainbowEnabled(editedColorsScheme, descriptor.getLanguage());
    boolean isInherited = false;
    // the colors are editable only for default language
    boolean isDefaultLanguage = descriptor.getLanguage() == null;
    boolean isEnable = !ColorAndFontOptions.isReadOnly(editedColorsScheme);
    if (rainbowOn == null) {
      isInherited = true;
      rainbowOn = RainbowHighlighter.isRainbowEnabled(editedColorsScheme, null);
    }
    myRainbow.setEnabled(isEnable && !isInherited);
    myRainbow.setSelected(rainbowOn);

    for (int i = 0; i < myLStops.length; ++i) {
      myLStops[i].setEnabled(isEnable && isDefaultLanguage);
      myStops[i].setEnabled(rainbowOn);
      myStops[i].setEditable(isEnable && isDefaultLanguage);
      myStops[i].setSelectedColor(editedColorsScheme.getAttributes(RainbowHighlighter.RAINBOW_COLOR_KEYS[i]).getForegroundColor());
    }

    myInheritanceLabel.setVisible(!isDefaultLanguage);
    myInheritAttributesBox.setEnabled(isEnable);
    myInheritAttributesBox.setSelected(isInherited);
    myInheritAttributesBox.setVisible(!isDefaultLanguage);
  }

  @Override
  public void apply(@NotNull EditorSchemeAttributeDescriptor attributeDescriptor, EditorColorsScheme scheme) {
    if (!(attributeDescriptor instanceof RainbowAttributeDescriptor)) return;
    RainbowAttributeDescriptor descriptor = (RainbowAttributeDescriptor)attributeDescriptor;

    boolean isDefaultLanguage = descriptor.getLanguage() == null;
    RainbowHighlighter.setRainbowEnabled(scheme,
                                         descriptor.getLanguage(),
                                         isDefaultLanguage ? Boolean.valueOf(myRainbow.isSelected())
                                                           : myInheritAttributesBox.isSelected() ? null
                                                                                                 : Boolean.valueOf(myRainbow.isSelected()));

    for (int i = 0; i < myStops.length; ++i) {
      scheme.setAttributes(RainbowHighlighter.RAINBOW_COLOR_KEYS[i], RainbowHighlighter.createRainbowAttribute(myStops[i].getSelectedColor()));
    }
    descriptor.apply(scheme);
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    myDispatcher.addListener(listener);
  }
}
