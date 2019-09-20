// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.colors;

import com.intellij.application.options.editor.fonts.AppEditorFontConfigurable;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.DelegatingFontPreferences;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FontOptions extends AbstractFontOptionsPanel {

  @NotNull private final ColorAndFontOptions myOptions;

  private @Nullable JCheckBox myOverwriteCheckBox;
  private @Nullable JLabel myBaseFontInfoLabel;

  private static final int FONT_PANEL_LEFT_OFFSET = 15;

  public FontOptions(@NotNull ColorAndFontOptions options) {
    myOptions = options;
  }

  @Nullable
  protected String getInheritedFontTitle() {
    return "default";
  }

  protected String getOverwriteFontTitle() {
    return ApplicationBundle.message("settings.editor.font.overwrite");
  }

  @Override
  protected JComponent createControls() {
    Component inheritBox = createOverwriteCheckBox();
    if (inheritBox != null) {
      JPanel topPanel = new JPanel(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      c.insets = JBUI.insets(BASE_INSET * 2, BASE_INSET, ADDITIONAL_VERTICAL_GAP, 0);
      c.anchor = GridBagConstraints.LINE_START;
      topPanel.add(inheritBox, c);
      c.gridy = 1;
      c.gridx = 0;
      c.gridwidth = 1;
      c.insets = JBUI.emptyInsets();
      topPanel.add(Box.createRigidArea(JBDimension.create(new Dimension(FONT_PANEL_LEFT_OFFSET, 0))), c);
      c.gridx = 1;
      c.anchor = GridBagConstraints.NORTHWEST;
      topPanel.add(createFontSettingsPanel(), c);
      return topPanel;
    }
    else {
      return super.createControls();
    }
  }

  @Nullable
  private Component createOverwriteCheckBox() {
    if (getInheritedFontTitle() != null) {
      JPanel overwritePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0,0 ));
      overwritePanel.setBorder(BorderFactory.createEmptyBorder());
      myOverwriteCheckBox = new JCheckBox(getOverwriteFontTitle() + " ");
      myOverwriteCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
      myOverwriteCheckBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          setDelegatingPreferences(!myOverwriteCheckBox.isSelected());
        }
      });
      overwritePanel.add(myOverwriteCheckBox);
      overwritePanel.add(createHyperlinkLabel());
      overwritePanel.add(grayed(new JLabel(" (")));
      myBaseFontInfoLabel = grayed(new JLabel("?"));
      overwritePanel.add(myBaseFontInfoLabel);
      overwritePanel.add(grayed(new JLabel(")")));
      return overwritePanel;
    }
    return null;
  }

  private static JLabel grayed(JLabel label) {
    label.setForeground(JBColor.GRAY);
    return label;
  }

  private String getBaseFontInfo() {
    FontPreferences basePrefs = getBaseFontPreferences();
    return basePrefs.getFontFamily() + ',' + basePrefs.getSize(basePrefs.getFontFamily());
  }

  protected FontPreferences getBaseFontPreferences() {
    return AppEditorFontOptions.getInstance().getFontPreferences();
  }

  @NotNull
  private JLabel createHyperlinkLabel() {
    HoverHyperlinkLabel label = new HoverHyperlinkLabel(getInheritedFontTitle());
    label.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          navigateToParentFontConfigurable();
        }
      }
    });
    return label;
  }

  protected void navigateToParentFontConfigurable() {
    Settings allSettings = Settings.KEY.getData(DataManager.getInstance().getDataContext(getPanel()));
    if (allSettings != null) {
      final Configurable fontConfigurable = allSettings.find(AppEditorFontConfigurable.ID);
      if (fontConfigurable != null) {
        allSettings.select(fontConfigurable);
      }
    }
  }

  @Override
  protected void setDelegatingPreferences(boolean isDelegating) {
    FontPreferences currPrefs = getCurrentScheme().getFontPreferences();
    if (currPrefs instanceof DelegatingFontPreferences == isDelegating) return;
    if (isDelegating) {
      getCurrentScheme().setUseAppFontPreferencesInEditor();
    }
    else {
      getCurrentScheme().setFontPreferences(getFontPreferences());
    }
    updateOptionsList();
    updateDescription(true);
  }

  @Override
  protected boolean isReadOnly() {
    return ColorAndFontOptions.isReadOnly(myOptions.getSelectedScheme());
  }

  @Override
  protected boolean isDelegating() {
    return getFontPreferences() instanceof DelegatingFontPreferences;
  }

  @NotNull
  @Override
  protected FontPreferences getFontPreferences() {
    return getCurrentScheme().getFontPreferences();
  }

  @Override
  protected void setFontSize(int fontSize) {
    getCurrentScheme().setEditorFontSize(fontSize);
  }

  @Override
  protected float getLineSpacing() {
    return getCurrentScheme().getLineSpacing();
  }

  @Override
  protected void setCurrentLineSpacing(float lineSpacing) {
    getCurrentScheme().setLineSpacing(lineSpacing);
  }

  protected EditorColorsScheme getCurrentScheme() {
    return myOptions.getSelectedScheme();
  }

  @Override
  protected void updateCustomOptions() {
    if (myOverwriteCheckBox != null) {
      myOverwriteCheckBox.setEnabled(!isReadOnly());
      myOverwriteCheckBox.setSelected(!isDelegating());
    }
    if (myBaseFontInfoLabel != null) {
      myBaseFontInfoLabel.setText(getBaseFontInfo());
    }
  }

}
