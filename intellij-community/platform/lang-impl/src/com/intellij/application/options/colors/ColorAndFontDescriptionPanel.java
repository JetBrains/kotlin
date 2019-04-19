// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptorWithPath;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.colors.AbstractKeyDescriptor;
import com.intellij.openapi.options.colors.ColorAndFontDescriptorsProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.BitUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.FontUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Map;

/**
 * @author cdr
 */
public class ColorAndFontDescriptionPanel extends JPanel implements OptionsPanelImpl.ColorDescriptionPanel {
  private final EventDispatcher<Listener> myDispatcher = EventDispatcher.create(Listener.class);

  private JPanel myPanel;

  private ColorPanel myBackgroundChooser;
  private ColorPanel myForegroundChooser;
  private ColorPanel myEffectsColorChooser;
  private ColorPanel myErrorStripeColorChooser;

  private JBCheckBox myCbBackground;
  private JBCheckBox myCbForeground;
  private JBCheckBox myCbEffects;
  private JBCheckBox myCbErrorStripe;

  private final Map<String, EffectType> myEffectsMap;
  {
    Map<String, EffectType> map = ContainerUtil.newLinkedHashMap();
    map.put(ApplicationBundle.message("combobox.effect.underscored"), EffectType.LINE_UNDERSCORE);
    map.put(ApplicationBundle.message("combobox.effect.boldunderscored"), EffectType.BOLD_LINE_UNDERSCORE);
    map.put(ApplicationBundle.message("combobox.effect.underwaved"), EffectType.WAVE_UNDERSCORE);
    map.put(ApplicationBundle.message("combobox.effect.bordered"), EffectType.BOXED);
    map.put(ApplicationBundle.message("combobox.effect.strikeout"), EffectType.STRIKEOUT);
    map.put(ApplicationBundle.message("combobox.effect.bold.dottedline"), EffectType.BOLD_DOTTED_LINE);
    myEffectsMap = Collections.unmodifiableMap(map);
  }
  private JComboBox myEffectsCombo;

  private JBCheckBox myCbBold;
  private JBCheckBox myCbItalic;
  private JLabel myLabelFont;
  private JTextPane myInheritanceLabel;

  private JBCheckBox myInheritAttributesBox;
  private boolean myUiEventsEnabled = true;

  public ColorAndFontDescriptionPanel() {
    super(new BorderLayout());
    add(myPanel, BorderLayout.CENTER);

    setBorder(JBUI.Borders.empty(4, 0, 4, 4));
    //noinspection unchecked
    myEffectsCombo.setModel(new CollectionComboBoxModel<>(ContainerUtil.newArrayList(myEffectsMap.keySet())));
    //noinspection unchecked
    myEffectsCombo.setRenderer(new ListCellRendererWrapper<String>() {
      @Override
      public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
        setText(value != null ? value : "<invalid>");
      }
    });

    ActionListener actionListener = e -> {
      if (myUiEventsEnabled) {
        myErrorStripeColorChooser.setEnabled(myCbErrorStripe.isSelected());
        myForegroundChooser.setEnabled(myCbForeground.isSelected());
        myBackgroundChooser.setEnabled(myCbBackground.isSelected());
        myEffectsColorChooser.setEnabled(myCbEffects.isSelected());
        myEffectsCombo.setEnabled(myCbEffects.isSelected());

        myDispatcher.getMulticaster().onSettingsChanged(e);
      }
    };

    for (JBCheckBox c : new JBCheckBox[]{myCbBackground, myCbForeground, myCbEffects, myCbErrorStripe, myCbItalic, myCbBold, myInheritAttributesBox}) {
      c.addActionListener(actionListener);
    }
    for (ColorPanel c : new ColorPanel[]{myBackgroundChooser, myForegroundChooser, myEffectsColorChooser, myErrorStripeColorChooser}) {
      c.addActionListener(actionListener);
    }
    myEffectsCombo.addActionListener(actionListener);

    Messages.configureMessagePaneUi(myInheritanceLabel, "<html>", null);
    myInheritanceLabel.addHyperlinkListener(e -> myDispatcher.getMulticaster().onHyperLinkClicked(e));
    myInheritanceLabel.setBorder(JBUI.Borders.empty(4, 0, 4, 4));
    myLabelFont.setVisible(false); // hide for now as it doesn't look that good
  }

  @NotNull
  @Override
  public JComponent getPanel() {
    return this;
  }

  @Override
  public void resetDefault() {
    try {
      myUiEventsEnabled = false;
      myLabelFont.setEnabled(false);
      myCbBold.setSelected(false);
      myCbBold.setEnabled(false);
      myCbItalic.setSelected(false);
      myCbItalic.setEnabled(false);
      updateColorChooser(myCbForeground, myForegroundChooser, false, false, null);
      updateColorChooser(myCbBackground, myBackgroundChooser, false, false, null);
      updateColorChooser(myCbErrorStripe, myErrorStripeColorChooser, false, false, null);
      updateColorChooser(myCbEffects, myEffectsColorChooser, false, false, null);
      myEffectsCombo.setEnabled(false);
      myInheritanceLabel.setVisible(false);
      myInheritAttributesBox.setVisible(false);
    }
    finally {
      myUiEventsEnabled = true;
    }
  }

  private static void updateColorChooser(JCheckBox checkBox,
                                         ColorPanel colorPanel,
                                         boolean isEnabled,
                                         boolean isChecked,
                                         @Nullable Color color) {
    checkBox.setEnabled(isEnabled);
    checkBox.setSelected(isChecked);
    if (color != null) {
      colorPanel.setSelectedColor(color);
    }
    else {
      colorPanel.setSelectedColor(JBColor.WHITE);
    }
    colorPanel.setEnabled(isChecked);
  }

  @Override
  public void reset(@NotNull EditorSchemeAttributeDescriptor attrDescription) {
    try {
      myUiEventsEnabled = false;
      if (!(attrDescription instanceof ColorAndFontDescription)) return;
      ColorAndFontDescription description = (ColorAndFontDescription)attrDescription;

      if (description.isFontEnabled()) {
        myLabelFont.setEnabled(description.isEditable());
        myCbBold.setEnabled(description.isEditable());
        myCbItalic.setEnabled(description.isEditable());
        int fontType = description.getFontType();
        myCbBold.setSelected(BitUtil.isSet(fontType, Font.BOLD));
        myCbItalic.setSelected(BitUtil.isSet(fontType, Font.ITALIC));
      }
      else {
        myLabelFont.setEnabled(false);
        myCbBold.setSelected(false);
        myCbBold.setEnabled(false);
        myCbItalic.setSelected(false);
        myCbItalic.setEnabled(false);
      }

      updateColorChooser(myCbForeground, myForegroundChooser, description.isForegroundEnabled(),
                         description.isForegroundChecked(), description.getForegroundColor());

      updateColorChooser(myCbBackground, myBackgroundChooser, description.isBackgroundEnabled(),
                         description.isBackgroundChecked(), description.getBackgroundColor());

      updateColorChooser(myCbErrorStripe, myErrorStripeColorChooser, description.isErrorStripeEnabled(),
                         description.isErrorStripeChecked(), description.getErrorStripeColor());

      EffectType effectType = description.getEffectType();
      updateColorChooser(myCbEffects, myEffectsColorChooser, description.isEffectsColorEnabled(),
                         description.isEffectsColorChecked(), description.getEffectColor());

      String name = ContainerUtil.reverseMap(myEffectsMap).get(effectType);
      myEffectsCombo.getModel().setSelectedItem(name);
      myEffectsCombo
        .setEnabled((description.isEffectsColorEnabled() && description.isEffectsColorChecked()) && description.isEditable());
      setInheritanceInfo(description);
      myLabelFont.setEnabled(myCbBold.isEnabled() || myCbItalic.isEnabled());
    }
    finally {
      myUiEventsEnabled = true;
    }
  }


  private void setInheritanceInfo(ColorAndFontDescription description) {
    Pair<ColorAndFontDescriptorsProvider, ? extends AbstractKeyDescriptor> baseDescriptor = description.getFallbackKeyDescriptor();
    if (baseDescriptor != null && baseDescriptor.second.getDisplayName() != null) {
      String attrName = baseDescriptor.second.getDisplayName();
      String attrLabel = attrName.replaceAll(EditorSchemeAttributeDescriptorWithPath.NAME_SEPARATOR, FontUtil.rightArrow(UIUtil.getLabelFont()));
      ColorAndFontDescriptorsProvider settingsPage = baseDescriptor.first;
      String style = "<div style=\"text-align:right\" vertical-align=\"top\">";
      String tooltipText;
      String labelText;
      if (settingsPage != null) {
        String pageName = settingsPage.getDisplayName();
        tooltipText = "Editor | Color Scheme | " + pageName + "<br>" + attrLabel;
        labelText = style + "<a href=\"" + pageName + "\">" + attrLabel + "</a><br>(" + pageName + ")";
      }
      else {
        tooltipText = attrLabel;
        labelText = style + attrLabel + "<br>&nbsp;";
      }

      myInheritanceLabel.setVisible(true);
      myInheritanceLabel.setText(labelText);
      myInheritanceLabel.getCaret().setDot(0);
      myInheritanceLabel.setToolTipText(tooltipText);
      myInheritanceLabel.setEnabled(true);
      myInheritAttributesBox.setVisible(true);
      myInheritAttributesBox.setEnabled(description.isEditable());
      myInheritAttributesBox.setSelected(description.isInherited());
      setEditEnabled(!description.isInherited() && description.isEditable(), description);
    }
    else {
      myInheritanceLabel.setVisible(false);
      myInheritAttributesBox.setSelected(false);
      myInheritAttributesBox.setVisible(false);
      setEditEnabled(description.isEditable(), description);
    }
  }

  private void setEditEnabled(boolean isEditEnabled, ColorAndFontDescription description) {
    myCbBackground.setEnabled(isEditEnabled && description.isBackgroundEnabled());
    myCbForeground.setEnabled(isEditEnabled && description.isForegroundEnabled());
    myCbBold.setEnabled(isEditEnabled && description.isFontEnabled());
    myCbItalic.setEnabled(isEditEnabled && description.isFontEnabled());
    myCbEffects.setEnabled(isEditEnabled && description.isEffectsColorEnabled());
    myCbErrorStripe.setEnabled(isEditEnabled && description.isErrorStripeEnabled());
    myErrorStripeColorChooser.setEditable(isEditEnabled);
    myEffectsColorChooser.setEditable(isEditEnabled);
    myForegroundChooser.setEditable(isEditEnabled);
    myBackgroundChooser.setEditable(isEditEnabled);
  }

  @Override
  public void apply(@NotNull EditorSchemeAttributeDescriptor attrDescription, EditorColorsScheme scheme) {
    if (!(attrDescription instanceof ColorAndFontDescription)) return;
    ColorAndFontDescription description = (ColorAndFontDescription)attrDescription;

    description.setInherited(myInheritAttributesBox.isSelected());
    if (description.isInherited()) {
      TextAttributes baseAttributes = description.getBaseAttributes();
      if (baseAttributes != null) {
        description.setFontType(baseAttributes.getFontType());
        description.setForegroundChecked(baseAttributes.getForegroundColor() != null);
        description.setForegroundColor(baseAttributes.getForegroundColor());
        description.setBackgroundChecked(baseAttributes.getBackgroundColor() != null);
        description.setBackgroundColor(baseAttributes.getBackgroundColor());
        description.setErrorStripeChecked(baseAttributes.getErrorStripeColor() != null);
        description.setErrorStripeColor(baseAttributes.getErrorStripeColor());
        description.setEffectColor(baseAttributes.getEffectColor());
        description.setEffectType(baseAttributes.getEffectType());
        description.setEffectsColorChecked(baseAttributes.getEffectColor() != null);
      }
      else {
        description.setInherited(false);
      }
      reset(description);
    }
    else {
      setInheritanceInfo(description);
      int fontType = Font.PLAIN;
      if (myCbBold.isSelected()) fontType |= Font.BOLD;
      if (myCbItalic.isSelected()) fontType |= Font.ITALIC;
      description.setFontType(fontType);
      description.setForegroundChecked(myCbForeground.isSelected());
      description.setForegroundColor(myForegroundChooser.getSelectedColor());
      description.setBackgroundChecked(myCbBackground.isSelected());
      description.setBackgroundColor(myBackgroundChooser.getSelectedColor());
      description.setErrorStripeChecked(myCbErrorStripe.isSelected());
      description.setErrorStripeColor(myErrorStripeColorChooser.getSelectedColor());
      description.setEffectsColorChecked(myCbEffects.isSelected());
      description.setEffectColor(myEffectsColorChooser.getSelectedColor());

      if (myEffectsCombo.isEnabled()) {
        String effectType = (String)myEffectsCombo.getModel().getSelectedItem();
        description.setEffectType(myEffectsMap.get(effectType));
      }
    }
    description.apply(scheme);
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    myDispatcher.addListener(listener);
  }

}
