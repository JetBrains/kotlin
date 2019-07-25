/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes.impl;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.TemplateLanguageFileType;
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author peter
 */
public class FileTypePatternDialog {
  private JTextField myPatternField;
  private JComboBox<Language> myLanguageCombo;
  private JLabel myTemplateDataLanguageButton;
  private JPanel myMainPanel;

  public FileTypePatternDialog(@Nullable String initialPatterns, FileType fileType, Language templateDataLanguage) {
    myPatternField.setText(initialPatterns);

    if (fileType instanceof TemplateLanguageFileType) {
      DefaultComboBoxModel<Language> model = (DefaultComboBoxModel<Language>)myLanguageCombo.getModel();
      model.addElement(null);
      List<Language> languages = TemplateDataLanguageMappings.getTemplateableLanguages();
      Collections.sort(languages, Comparator.comparing(Language::getID));
      for (Language language : languages) {
        model.addElement(language);
      }
      myLanguageCombo.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
        label.setText(value == null ? "" : value.getDisplayName());
        if (value != null) {
          FileType type = value.getAssociatedFileType();
          if (type != null) {
            label.setIcon(type.getIcon());
          }
        }
      }));
      myLanguageCombo.setSelectedItem(templateDataLanguage);
    } else {
      myLanguageCombo.setVisible(false);
      myTemplateDataLanguageButton.setVisible(false);
    }
  }

  public JTextField getPatternField() {
    return myPatternField;
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  public Language getTemplateDataLanguage() {
    return (Language)myLanguageCombo.getSelectedItem();
  }
}
