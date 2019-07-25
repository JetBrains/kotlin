// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.diagnostic.logging;

import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.UIBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class EditLogPatternDialog extends DialogWrapper {

  private JPanel myWholePanel;
  private JTextField myNameField;
  private JCheckBox myShowFilesCombo;
  private TextFieldWithBrowseButton myFilePattern;

  public EditLogPatternDialog() {
    super(true);
    setTitle(DiagnosticBundle.message("log.monitor.edit.aliases.title"));
    init();
  }

  public void init(String name, String pattern, boolean showAll){
    myNameField.setText(name);
    myFilePattern.setText(pattern);
    myShowFilesCombo.setSelected(showAll);
    setOKActionEnabled(pattern != null && pattern.length() > 0);
  }

  @Override
  protected JComponent createCenterPanel() {
    myFilePattern.addBrowseFolderListener(UIBundle.message("file.chooser.default.title"), null, null, FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    myFilePattern.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        setOKActionEnabled(myFilePattern.getText() != null && myFilePattern.getText().length() > 0);
      }
    });
    return myWholePanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  public boolean isShowAllFiles() {
    return myShowFilesCombo.isSelected();
  }

  public String getName(){
    final String name = myNameField.getText();
    if (name != null && name.length() > 0){
      return name;
    }
    return myFilePattern.getText();
  }

  public String getLogPattern(){
    return myFilePattern.getText();
  }


  @Override
  protected String getHelpId() {
    return "reference.run.configuration.edit.logfile.aliases";
  }
}
