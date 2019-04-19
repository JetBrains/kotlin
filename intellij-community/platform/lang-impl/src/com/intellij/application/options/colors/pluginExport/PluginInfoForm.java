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
package com.intellij.application.options.colors.pluginExport;

import com.intellij.openapi.editor.colors.EditorColorsUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PluginInfoForm {
  private JTextField myVendorMailField;
  private JTextField myVendorNameField;
  private JBTextField myVendorUrl;
  private JBTextField myVersionField;
  private EditorTextField myDescriptionArea;
  private EditorTextField myChangeNotesArea;
  private JPanel myTopPanel;

  public JPanel getTopPanel() {
    return myTopPanel;
  }

  public void init(@NotNull PluginExportData exportData) {
    myVendorMailField.setText(exportData.getVendorMail());
    myVendorNameField.setText(exportData.getVendorName());
    myVendorUrl.setText(exportData.getVendorUrl());
    myVersionField.setText(exportData.getPluginVersion());
    myDescriptionArea.setText(exportData.getDescription());
    myChangeNotesArea.setText(exportData.getChangeNotes());
  }

  public void apply(@NotNull PluginExportData exportData) {
    exportData.setVendorMail(myVendorMailField.getText());
    exportData.setVendorName(myVendorNameField.getText());
    exportData.setVendorUrl(myVendorUrl.getText());
    exportData.setPluginVersion(myVersionField.getText());
    exportData.setDescription(myDescriptionArea.getText());
    exportData.setChangeNotes(myChangeNotesArea.getText());
  }

  private void createUIComponents() {
    myDescriptionArea = createDescriptionEditor();
    myChangeNotesArea = createDescriptionEditor();
  }

  private static EditorTextField createDescriptionEditor() {
    EditorTextField descriptionEditor = new EditorTextField() {
      @Override
      protected EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        editor.getSettings().setUseSoftWraps(true);
        editor.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        editor.setColorsScheme(EditorColorsUtil.getColorSchemeForComponent(this));
        return editor;
      }
    };
    descriptionEditor.setOneLineMode(false);
    descriptionEditor.setPreferredSize(new Dimension(descriptionEditor.getWidth(), JBUI.scale(100)));
    return descriptionEditor;
  }
}
