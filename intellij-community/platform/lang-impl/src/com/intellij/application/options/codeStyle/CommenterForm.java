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
package com.intellij.application.options.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Reusable commenter settings form.
 */
public class CommenterForm implements CodeStyleSettingsCustomizable {
  private JPanel myCommenterPanel;
  private JBCheckBox myLineCommentAtFirstColumnCb;
  private JBCheckBox myLineCommentAddSpaceCb;
  private JBCheckBox myBlockCommentAtFirstJBCheckBox;
  
  private final Language myLanguage;

  public CommenterForm(Language language) {
    this(language, ApplicationBundle.message("title.naming.comment.code"));
  }

  public CommenterForm(Language language, @Nullable String title) {
    myLanguage = language;
    if (title != null) {
      myCommenterPanel.setBorder(IdeBorderFactory.createTitledBorder(title));
    }
    myLineCommentAtFirstColumnCb.addActionListener(e -> {
      if (myLineCommentAtFirstColumnCb.isSelected()) {
        myLineCommentAddSpaceCb.setSelected(false);
      }
      myLineCommentAddSpaceCb.setEnabled(!myLineCommentAtFirstColumnCb.isSelected());
    });
    customizeSettings();
  }

  public void reset(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    myLineCommentAtFirstColumnCb.setSelected(langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
    myBlockCommentAtFirstJBCheckBox.setSelected(langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN);
    myLineCommentAddSpaceCb.setSelected(langSettings.LINE_COMMENT_ADD_SPACE && !langSettings.LINE_COMMENT_AT_FIRST_COLUMN);
    myLineCommentAddSpaceCb.setEnabled(!langSettings .LINE_COMMENT_AT_FIRST_COLUMN);
  }
  
  
  public void apply(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    langSettings.LINE_COMMENT_AT_FIRST_COLUMN = myLineCommentAtFirstColumnCb.isSelected();
    langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = myBlockCommentAtFirstJBCheckBox.isSelected();
    langSettings.LINE_COMMENT_ADD_SPACE = myLineCommentAddSpaceCb.isSelected();
  }
  
  public boolean isModified(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    return myLineCommentAtFirstColumnCb.isSelected() != langSettings.LINE_COMMENT_AT_FIRST_COLUMN
           || myBlockCommentAtFirstJBCheckBox.isSelected() != langSettings.BLOCK_COMMENT_AT_FIRST_COLUMN
           || myLineCommentAddSpaceCb.isSelected() != langSettings.LINE_COMMENT_ADD_SPACE;
  }

  public JPanel getCommenterPanel() {
    return myCommenterPanel;
  }

  @Override
  public void showAllStandardOptions() {
    setAllOptionsVisible(true);
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    for (String optionName : optionNames) {
      if (CommenterOption.LINE_COMMENT_ADD_SPACE.name().equals(optionName)) {
        myLineCommentAddSpaceCb.setVisible(true);
      }
      else if (CommenterOption.LINE_COMMENT_AT_FIRST_COLUMN.name().equals(optionName)) {
        myLineCommentAtFirstColumnCb.setVisible(true);
      }
      else if (CommenterOption.BLOCK_COMMENT_AT_FIRST_COLUMN.name().equals(optionName)) {
        myBlockCommentAtFirstJBCheckBox.setVisible(true);
      }
    }
  }
  
  private void setAllOptionsVisible(boolean isVisible) {
    myLineCommentAtFirstColumnCb.setVisible(isVisible);
    myLineCommentAddSpaceCb.setVisible(isVisible);
    myBlockCommentAtFirstJBCheckBox.setVisible(isVisible);
  }
  
  private void customizeSettings() {
    setAllOptionsVisible(false);
    LanguageCodeStyleSettingsProvider settingsProvider = LanguageCodeStyleSettingsProvider.forLanguage(myLanguage);
    if (settingsProvider != null) {
      settingsProvider.customizeSettings(this, LanguageCodeStyleSettingsProvider.SettingsType.COMMENTER_SETTINGS);
    }
    myCommenterPanel.setVisible(
      myLineCommentAtFirstColumnCb.isVisible() || myLineCommentAddSpaceCb.isVisible() || myBlockCommentAtFirstJBCheckBox.isVisible());
  }
}
