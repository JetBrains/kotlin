// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.diff.impl.settings;

import com.intellij.application.options.colors.ColorAndFontDescription;
import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.application.options.colors.OptionsPanelImpl;
import com.intellij.diff.util.TextDiffTypeFactory;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class DiffColorDescriptionPanel extends JPanel implements OptionsPanelImpl.ColorDescriptionPanel {
  private final EventDispatcher<Listener> myDispatcher = EventDispatcher.create(Listener.class);

  private JPanel myPanel;

  private ColorPanel myBackgroundColorPanel;
  private ColorPanel myIgnoredColorPanel;
  private ColorPanel myStripeMarkColorPanel;
  private JBCheckBox myInheritIgnoredCheckBox;

  @NotNull private final ColorAndFontOptions myOptions;

  DiffColorDescriptionPanel(@NotNull ColorAndFontOptions options) {
    super(new BorderLayout());
    myOptions = options;
    add(myPanel, BorderLayout.CENTER);

    myBackgroundColorPanel.addActionListener(this::onSettingsChanged);
    myIgnoredColorPanel.addActionListener(this::onSettingsChanged);
    myStripeMarkColorPanel.addActionListener(this::onSettingsChanged);

    myInheritIgnoredCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myIgnoredColorPanel.setEnabled(!myInheritIgnoredCheckBox.isSelected());

        if (myInheritIgnoredCheckBox.isSelected()) {
          myIgnoredColorPanel.setSelectedColor(null);
        }
        else {
          Color background = ObjectUtils.notNull(myBackgroundColorPanel.getSelectedColor(), JBColor.WHITE);
          Color gutterBackground = myOptions.getSelectedScheme().getDefaultBackground();
          myIgnoredColorPanel.setSelectedColor(TextDiffTypeFactory.getMiddleColor(background, gutterBackground));
        }

        onSettingsChanged(e);
      }
    });
  }

  @NotNull
  @Override
  public JComponent getPanel() {
    return this;
  }

  private void onSettingsChanged(@NotNull ActionEvent e) {
    myDispatcher.getMulticaster().onSettingsChanged(e);
  }

  @Override
  public void resetDefault() {
    myBackgroundColorPanel.setEnabled(false);
    myIgnoredColorPanel.setEnabled(false);
    myStripeMarkColorPanel.setEnabled(false);
    myInheritIgnoredCheckBox.setEnabled(false);
    myInheritIgnoredCheckBox.setSelected(false);
  }

  @Override
  public void reset(@NotNull EditorSchemeAttributeDescriptor attrDescription) {
    if (!(attrDescription instanceof ColorAndFontDescription)) return;
    ColorAndFontDescription description = (ColorAndFontDescription)attrDescription;

    Color backgroundColor = getBackgroundColor(description);
    Color ignoredColor = getIgnoredColor(description);
    Color stripeMarkColor = getStripeMarkColor(description);
    boolean inheritIgnored = ignoredColor == null;

    myBackgroundColorPanel.setEnabled(true);
    myIgnoredColorPanel.setEnabled(!inheritIgnored);
    myStripeMarkColorPanel.setEnabled(true);
    myInheritIgnoredCheckBox.setEnabled(true);

    myBackgroundColorPanel.setSelectedColor(backgroundColor);
    myIgnoredColorPanel.setSelectedColor(ignoredColor);
    myStripeMarkColorPanel.setSelectedColor(stripeMarkColor);
    myInheritIgnoredCheckBox.setSelected(inheritIgnored);
  }

  @Override
  public void apply(@NotNull EditorSchemeAttributeDescriptor attrDescription, EditorColorsScheme scheme) {
    if (!(attrDescription instanceof ColorAndFontDescription)) return;
    ColorAndFontDescription description = (ColorAndFontDescription)attrDescription;

    description.setBackgroundChecked(true);
    description.setForegroundChecked(true);
    description.setErrorStripeChecked(true);

    setBackgroundColor(description);
    setIgnoredColor(description);
    setStripeMarkColor(description);

    description.apply(scheme);
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    myDispatcher.addListener(listener);
  }

  @Nullable
  private static Color getBackgroundColor(@NotNull TextAttributes attributes) {
    return attributes.getBackgroundColor();
  }

  @Nullable
  private static Color getIgnoredColor(@NotNull TextAttributes attributes) {
    return attributes.getForegroundColor();
  }

  @Nullable
  private static Color getStripeMarkColor(@NotNull TextAttributes attributes) {
    return attributes.getErrorStripeColor();
  }

  private void setBackgroundColor(@NotNull TextAttributes attributes) {
    attributes.setBackgroundColor(myBackgroundColorPanel.getSelectedColor());
  }

  private void setIgnoredColor(@NotNull TextAttributes attributes) {
    attributes.setForegroundColor(myInheritIgnoredCheckBox.isSelected() ? null : myIgnoredColorPanel.getSelectedColor());
  }

  private void setStripeMarkColor(@NotNull TextAttributes attributes) {
    attributes.setErrorStripeColor(myStripeMarkColorPanel.getSelectedColor());
  }
}
