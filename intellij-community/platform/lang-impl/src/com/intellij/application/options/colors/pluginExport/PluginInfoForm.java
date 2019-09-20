// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors.pluginExport;

import com.intellij.openapi.editor.colors.EditorColorsUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.scale.JBUIScale;
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
    descriptionEditor.setPreferredSize(new Dimension(descriptionEditor.getWidth(), JBUIScale.scale(100)));
    return descriptionEditor;
  }
}
