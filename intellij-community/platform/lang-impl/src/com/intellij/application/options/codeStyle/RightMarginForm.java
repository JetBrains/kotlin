// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.psi.codeStyle.CodeStyleConstraints;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.fields.CommaSeparatedIntegersField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.fields.valueEditors.CommaSeparatedIntegersValueEditor;
import com.intellij.ui.components.fields.valueEditors.ValueEditor;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Can be used for languages which do not use standard "Wrapping and Braces" panel.
 * <p>
 * <strong>Note</strong>: besides adding the panel to UI it is necessary to make sure that language's own
 * {@code LanguageCodeStyleSettingsProvider} explicitly supports RIGHT_MARGIN field in {@code customizeSettings()}
 * method as shown below:
 * <pre>
 * public void customizeSettings(...) {
 *   if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
 *     consumer.showStandardOptions("RIGHT_MARGIN");
 *   }
 * }
 * </pre>
 * @author Rustam Vishnyakov
 */
public class RightMarginForm {
  private IntegerField myRightMarginField;
  private JPanel myTopPanel;
  private JComboBox<String> myWrapOnTypingCombo;
  private CommaSeparatedIntegersField myVisualGuidesField;
  @SuppressWarnings("unused") private ActionLink myResetLink;
  private JLabel myVisualGuidesHint;
  private JLabel myVisualGuidesLabel;
  private ActionLink myResetGuidesLink;
  private final Language myLanguage;
  private final CodeStyleSettings mySettings;

  public RightMarginForm(@NotNull Language language, @NotNull CodeStyleSettings settings) {
    myLanguage = language;
    mySettings = settings;

    //noinspection unchecked
    myWrapOnTypingCombo.setModel(new DefaultComboBoxModel(
      CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS
    ));
    MarginOptionsUtil.customizeWrapOnTypingCombo(myWrapOnTypingCombo, settings);
    myVisualGuidesHint.setForeground(JBColor.GRAY);
    myVisualGuidesHint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    myVisualGuidesLabel.setText(ApplicationBundle.message("settings.code.style.visual.guides") + ":");
  }

  void createUIComponents() {
    myRightMarginField = new IntegerField(ApplicationBundle.message("editbox.right.margin.columns"),
                                          0, CodeStyleConstraints.MAX_RIGHT_MARGIN);
    myRightMarginField.getValueEditor().addListener(new ValueEditor.Listener<Integer>() {
      @Override
      public void valueChanged(@NotNull Integer newValue) {
        myResetLink.setVisible(!newValue.equals(myRightMarginField.getDefaultValue()));
        myRightMarginField.getEmptyText().setText(MarginOptionsUtil.getDefaultRightMarginText(mySettings));
      }
    });
    myRightMarginField.setCanBeEmpty(true);
    myRightMarginField.setDefaultValue(-1);
    myRightMarginField.setMinimumSize(new Dimension(JBUIScale.scale(120), myRightMarginField.getMinimumSize().height));
    myVisualGuidesField = new CommaSeparatedIntegersField(ApplicationBundle.message("settings.code.style.visual.guides"), 0, CodeStyleConstraints.MAX_RIGHT_MARGIN, "Optional");
    myVisualGuidesField.getValueEditor().addListener(new ValueEditor.Listener<List<Integer>>() {
      @Override
      public void valueChanged(@NotNull List<Integer> newValue) {
        myResetGuidesLink.setVisible(!myVisualGuidesField.isEmpty());
        myVisualGuidesField.getEmptyText().setText(getDefaultVisualGuidesText(mySettings));
      }
    });
    myResetLink = new ActionLink("Reset", new ResetRightMarginAction());
    myVisualGuidesLabel = new JLabel();
    myResetGuidesLink = new ActionLink("Reset", new ResetGuidesAction());
  }

  private class ResetRightMarginAction extends DumbAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myRightMarginField.resetToDefault();
    }
  }

  private class ResetGuidesAction extends DumbAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myVisualGuidesField.clear();
    }
  }

  public void reset(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    myRightMarginField.setValue(langSettings.RIGHT_MARGIN);
    for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length; i ++) {
      if (langSettings.WRAP_ON_TYPING == CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i]) {
        myWrapOnTypingCombo.setSelectedIndex(i);
        break;
      }
    }
    myVisualGuidesField.setValue(langSettings.getSoftMargins());
    myResetLink.setVisible(langSettings.RIGHT_MARGIN >= 0);
    myResetGuidesLink.setVisible(!langSettings.getSoftMargins().isEmpty());
    myRightMarginField.getEmptyText().setText(MarginOptionsUtil.getDefaultRightMarginText(settings));
    myVisualGuidesField.getEmptyText().setText(getDefaultVisualGuidesText(settings));
  }

  public void apply(@NotNull CodeStyleSettings settings) throws ConfigurationException {
    myRightMarginField.validateContent();
    myVisualGuidesField.validateContent();
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    langSettings.RIGHT_MARGIN = myRightMarginField.getValue();
    langSettings.WRAP_ON_TYPING = getSelectedWrapOnTypingValue();
    settings.setSoftMargins(myLanguage, myVisualGuidesField.getValue());
  }

  public boolean isModified(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    return langSettings.RIGHT_MARGIN != myRightMarginField.getValue() ||
           langSettings.WRAP_ON_TYPING != getSelectedWrapOnTypingValue() ||
           !langSettings.getSoftMargins().equals(myVisualGuidesField.getValue());
  }


  private int getSelectedWrapOnTypingValue() {
    int i = myWrapOnTypingCombo.getSelectedIndex();
    if (i >= 0 && i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length) {
      return CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i];
    }
    return CommonCodeStyleSettings.WrapOnTyping.DEFAULT.intValue;
  }

  public JPanel getTopPanel() {
    return myTopPanel;
  }

  private static String getDefaultVisualGuidesText(@NotNull CodeStyleSettings settings) {
    List<Integer> margins = settings.getDefaultSoftMargins();
    String marginsString =
      margins.size() <= 2 ?
      CommaSeparatedIntegersValueEditor.intListToString(margins) :
      CommaSeparatedIntegersValueEditor.intListToString(margins.subList(0, 2)) + ",...";
    return MarginOptionsUtil.getDefaultValueText(marginsString);
  }

}
