/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.editor.fonts;

import com.intellij.application.options.colors.AbstractFontOptionsPanel;
import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.application.options.colors.ColorAndFontSettingsListener;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.ModifiableFontPreferences;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.editor.colors.impl.FontPreferencesImpl;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AppEditorFontOptionsPanel extends AbstractFontOptionsPanel {
  private final EditorColorsScheme myScheme;
  private JPanel myWarningPanel;
  private JLabel myEditorFontLabel;
  private JButton myRestoreButton;
  private final FontPreferences myDefaultPreferences;

  protected AppEditorFontOptionsPanel(EditorColorsScheme scheme) {
    myScheme = scheme;
    myDefaultPreferences = new FontPreferencesImpl();
    AppEditorFontOptions.initDefaults((ModifiableFontPreferences)myDefaultPreferences);
    updateOptionsList();
  }

  @Override
  protected JComponent createControls() {
    JPanel topPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = JBUI.emptyInsets();
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.WEST;
    myWarningPanel = createMessagePanel();
    topPanel.add(myWarningPanel, c);
    c.gridy = 1;
    topPanel.add(createFontSettingsPanel(), c);
    c.insets = JBUI.insets(5, 0, 0, 0);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridy = 2;
    topPanel.add(new JSeparator(), c);
    c.gridy = 3;
    c.fill = GridBagConstraints.NONE;
    myRestoreButton = new JButton(ApplicationBundle.message("settings.editor.font.restored.defaults"));
    myRestoreButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        restoreDefaults();
      }
    });
    addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void fontChanged() {
        updateWarning();
        updateRestoreButtonState();
      }
    });
    topPanel.add(myRestoreButton, c);
    return topPanel;
  }

  private void restoreDefaults() {
    AppEditorFontOptions.initDefaults((ModifiableFontPreferences)getFontPreferences());
    updateOnChangedFont();
  }

  public void updateOnChangedFont() {
    updateOptionsList();
    fireFontChanged();
  }

  private void updateRestoreButtonState() {
    myRestoreButton.setEnabled(!myDefaultPreferences.equals(getFontPreferences()));
  }

  private JPanel createMessagePanel() {
    JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    messagePanel.add(new JLabel(AllIcons.General.BalloonWarning));
    myEditorFontLabel = createHyperlinkLabel();
    messagePanel.add(myEditorFontLabel);
    JLabel commentLabel = new JLabel(ApplicationBundle.message("settings.editor.font.defined.in.color.scheme.message"));
    commentLabel.setForeground(JBColor.GRAY);
    messagePanel.add(commentLabel);
    return messagePanel;
  }

  public void updateWarning() {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    if (!scheme.isUseAppFontPreferencesInEditor()) {
      myEditorFontLabel.setText(
        ApplicationBundle.message("settings.editor.font.overridden.message", scheme.getEditorFontName(), scheme.getEditorFontSize()));
      myWarningPanel.setVisible(true);
    }
    else {
      myWarningPanel.setVisible(false);
    }
  }

  @Override
  protected boolean isReadOnly() {
    return false;
  }

  @Override
  protected boolean isDelegating() {
    return false;
  }

  @NotNull
  @Override
  protected FontPreferences getFontPreferences() {
    return myScheme.getFontPreferences();
  }

  @Override
  protected void setFontSize(int fontSize) {
    myScheme.setEditorFontSize(fontSize);
  }

  @Override
  protected float getLineSpacing() {
    return myScheme.getLineSpacing();
  }

  @Override
  protected void setCurrentLineSpacing(float lineSpacing) {
    myScheme.setLineSpacing(lineSpacing);
  }

  @NotNull
  private JLabel createHyperlinkLabel() {
    HoverHyperlinkLabel label = new HoverHyperlinkLabel("");
    label.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          navigateToColorSchemeFontConfigurable();
        }
      }
    });
    return label;
  }

  protected void navigateToColorSchemeFontConfigurable() {
    Settings allSettings = Settings.KEY.getData(DataManager.getInstance().getDataContext(getPanel()));
    if (allSettings != null) {
      final Configurable colorSchemeConfigurable = allSettings.find(ColorAndFontOptions.ID);
      if (colorSchemeConfigurable instanceof ColorAndFontOptions) {
        Configurable fontOptions =
          ((ColorAndFontOptions)colorSchemeConfigurable).findSubConfigurable(ColorAndFontOptions.FONT_CONFIGURABLE_NAME);
        if (fontOptions != null) {
          allSettings.select(fontOptions);
        }
      }
    }
  }
}
