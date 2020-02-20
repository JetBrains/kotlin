// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeEditor.printing;

import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.OptionGroup;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ExportToHTMLDialog extends DialogWrapper {
  private JRadioButton myRbCurrentFile;
  private JRadioButton myRbSelectedText;
  private JRadioButton myRbCurrentPackage;
  private JCheckBox myCbIncludeSubpackages;
  private JCheckBox myCbLineNumbers;

  private JCheckBox myCbOpenInBrowser;
  private TextFieldWithBrowseButton myTargetDirectoryField;
  private final String myFileName;
  private final String myDirectoryName;
  private final boolean myIsSelectedTextEnabled;
  private final Project myProject;
  private final List<UnnamedConfigurable> myExtensions;

  public ExportToHTMLDialog(String fileName, String directoryName, boolean isSelectedTextEnabled, Project project) {
    super(project, true);
    myProject = project;
    setOKButtonText(EditorBundle.message("export.to.html.save.button"));
    myFileName = fileName;
    myDirectoryName = directoryName;
    this.myIsSelectedTextEnabled = isSelectedTextEnabled;
    setTitle(EditorBundle.message("export.to.html.title"));
    myExtensions = ContainerUtil.map(PrintOption.EP_NAME.getExtensionList(), PrintOption::createConfigurable);
    init();
  }

  @Override
  protected JComponent createNorthPanel() {
    OptionGroup optionGroup = new OptionGroup();

    Object @NotNull [] params1 = new Object[]{(myFileName != null ? myFileName : "")};
    myRbCurrentFile = new JRadioButton(EditorBundle.message("export.to.html.file.name.radio", params1));
    optionGroup.add(myRbCurrentFile);

    myRbSelectedText = new JRadioButton(EditorBundle.message("export.to.html.selected.text.radio"));
    optionGroup.add(myRbSelectedText);

    Object @NotNull [] params = new Object[]{(myDirectoryName != null ? myDirectoryName : "")};
    myRbCurrentPackage = new JRadioButton(
      EditorBundle.message("export.to.html.all.files.in.directory.radio", params));
    optionGroup.add(myRbCurrentPackage);

    myCbIncludeSubpackages = new JCheckBox(EditorBundle.message("export.to.html.include.subdirectories.checkbox"));
    optionGroup.add(myCbIncludeSubpackages, true);

    FileTextField field = FileChooserFactory.getInstance().createFileTextField(FileChooserDescriptorFactory.createSingleFolderDescriptor(), myDisposable);
    myTargetDirectoryField = new TextFieldWithBrowseButton(field.getField());
    LabeledComponent<TextFieldWithBrowseButton> labeledComponent = assignLabel(myTargetDirectoryField, myProject);

    optionGroup.add(labeledComponent);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.add(myRbCurrentFile);
    buttonGroup.add(myRbSelectedText);
    buttonGroup.add(myRbCurrentPackage);

    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myCbIncludeSubpackages.setEnabled(myRbCurrentPackage.isSelected());
      }
    };

    myRbCurrentFile.addActionListener(actionListener);
    myRbSelectedText.addActionListener(actionListener);
    myRbCurrentPackage.addActionListener(actionListener);

    return optionGroup.createPanel();
  }

  public static LabeledComponent<TextFieldWithBrowseButton> assignLabel(TextFieldWithBrowseButton targetDirectoryField, Project project) {
    LabeledComponent<TextFieldWithBrowseButton> labeledComponent = new LabeledComponent<>();
    labeledComponent.setText(EditorBundle.message("export.to.html.output.directory.label"));
    targetDirectoryField.addBrowseFolderListener(EditorBundle.message("export.to.html.select.output.directory.title"),
                                                 EditorBundle.message("export.to.html.select.output.directory.description"),
                                                 project, FileChooserDescriptorFactory.createSingleFolderDescriptor());
    labeledComponent.setComponent(targetDirectoryField);
    return labeledComponent;
  }

  @Override
  protected JComponent createCenterPanel() {
    OptionGroup optionGroup = new OptionGroup(EditorBundle.message("export.to.html.options.group"));

    myCbLineNumbers = new JCheckBox(EditorBundle.message("export.to.html.options.show.line.numbers.checkbox"));
    optionGroup.add(myCbLineNumbers);

    for (UnnamedConfigurable printOption : myExtensions) {
      optionGroup.add(printOption.createComponent());
    }

    myCbOpenInBrowser = new JCheckBox(EditorBundle.message("export.to.html.open.generated.html.checkbox"));
    optionGroup.add(myCbOpenInBrowser);

    return optionGroup.createPanel();
  }

  public void reset() {
    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myProject);

    myRbSelectedText.setEnabled(myIsSelectedTextEnabled);
    myRbSelectedText.setSelected(myIsSelectedTextEnabled);
    myRbCurrentFile.setEnabled(myFileName != null);
    myRbCurrentFile.setSelected(myFileName != null && !myIsSelectedTextEnabled);
    myRbCurrentPackage.setEnabled(myDirectoryName != null);
    myRbCurrentPackage.setSelected(myDirectoryName != null && !myIsSelectedTextEnabled && myFileName == null);
    myCbIncludeSubpackages.setSelected(exportToHTMLSettings.isIncludeSubdirectories());
    myCbIncludeSubpackages.setEnabled(myRbCurrentPackage.isSelected());

    myCbLineNumbers.setSelected(exportToHTMLSettings.PRINT_LINE_NUMBERS);
    myCbOpenInBrowser.setSelected(exportToHTMLSettings.OPEN_IN_BROWSER);

    myTargetDirectoryField.setText(exportToHTMLSettings.OUTPUT_DIRECTORY);

    for (UnnamedConfigurable printOption : myExtensions) {
      printOption.reset();
    }
  }

  @Override
  protected void dispose() {
    for (UnnamedConfigurable extension : myExtensions) {
      extension.disposeUIResources();
    }
    super.dispose();
  }

  public void apply() throws ConfigurationException {
    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myProject);

    if (myRbCurrentFile.isSelected()){
      exportToHTMLSettings.setPrintScope(PrintSettings.PRINT_FILE);
    }
    else if (myRbSelectedText.isSelected()){
      exportToHTMLSettings.setPrintScope(PrintSettings.PRINT_SELECTED_TEXT);
    }
    else if (myRbCurrentPackage.isSelected()){
      exportToHTMLSettings.setPrintScope(PrintSettings.PRINT_DIRECTORY);
    }

    exportToHTMLSettings.setIncludeSubpackages(myCbIncludeSubpackages.isSelected());
    exportToHTMLSettings.PRINT_LINE_NUMBERS = myCbLineNumbers.isSelected();
    exportToHTMLSettings.OPEN_IN_BROWSER = myCbOpenInBrowser.isSelected();
    exportToHTMLSettings.OUTPUT_DIRECTORY = myTargetDirectoryField.getText();
    for (UnnamedConfigurable printOption : myExtensions) {
      printOption.apply();
    }
  }

  @Override
  protected String getHelpId() {
    return HelpID.EXPORT_TO_HTML;
  }
}