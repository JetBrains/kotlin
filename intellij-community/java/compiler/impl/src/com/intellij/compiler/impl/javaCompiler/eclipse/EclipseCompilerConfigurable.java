// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl.javaCompiler.eclipse;

import com.intellij.compiler.impl.javaCompiler.CompilerModuleOptionsComponent;
import com.intellij.compiler.options.ComparingUtils;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.jps.model.java.compiler.EclipseCompilerOptions;

import javax.swing.*;

/**
 * @author cdr
 */
public class EclipseCompilerConfigurable implements Configurable {
  private final Project myProject;
  private JPanel myPanel;
  private JCheckBox myCbDeprecation;
  private JCheckBox myCbDebuggingInfo;
  private JCheckBox myCbGenerateNoWarnings;
  private RawCommandLineEditor myAdditionalOptionsField;
  private JCheckBox myCbProceedOnErrors;
  private CompilerModuleOptionsComponent myOptionsOverride;
  private TextFieldWithBrowseButton myPathToEcjField;
  private final EclipseCompilerOptions myCompilerSettings;

  public EclipseCompilerConfigurable(Project project, EclipseCompilerOptions options) {
    myProject = project;
    myCompilerSettings = options;
    myAdditionalOptionsField.setDialogCaption(CompilerBundle.message("java.compiler.option.additional.command.line.parameters"));
    myAdditionalOptionsField.setDescriptor(null, false);
    myPathToEcjField.addBrowseFolderListener(
      "Path to ecj compiler tool", null, project,
      new FileChooserDescriptor(true, false, true, true, false, false).withFileFilter(file -> FileTypeRegistry.getInstance().isFileOfType(file, ArchiveFileType.INSTANCE))
    );
  }

  private void createUIComponents() {
    myOptionsOverride = new CompilerModuleOptionsComponent(myProject);
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    boolean isModified = false;

    isModified |= ComparingUtils.isModified(myCbDeprecation, myCompilerSettings.DEPRECATION);
    isModified |= ComparingUtils.isModified(myCbDebuggingInfo, myCompilerSettings.DEBUGGING_INFO);
    isModified |= ComparingUtils.isModified(myCbGenerateNoWarnings, myCompilerSettings.GENERATE_NO_WARNINGS);
    isModified |= ComparingUtils.isModified(myCbProceedOnErrors, myCompilerSettings.PROCEED_ON_ERROR);
    isModified |= ComparingUtils.isModified(myPathToEcjField, FileUtil.toSystemDependentName(myCompilerSettings.ECJ_TOOL_PATH));
    isModified |= ComparingUtils.isModified(myAdditionalOptionsField, myCompilerSettings.ADDITIONAL_OPTIONS_STRING);
    isModified |= !myOptionsOverride.getModuleOptionsMap().equals(myCompilerSettings.ADDITIONAL_OPTIONS_OVERRIDE);

    return isModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    myCompilerSettings.DEPRECATION =  myCbDeprecation.isSelected();
    myCompilerSettings.DEBUGGING_INFO = myCbDebuggingInfo.isSelected();
    myCompilerSettings.GENERATE_NO_WARNINGS = myCbGenerateNoWarnings.isSelected();
    myCompilerSettings.PROCEED_ON_ERROR = myCbProceedOnErrors.isSelected();
    myCompilerSettings.ECJ_TOOL_PATH = FileUtil.toSystemIndependentName(myPathToEcjField.getText().trim());
    myCompilerSettings.ADDITIONAL_OPTIONS_STRING = myAdditionalOptionsField.getText();
    myCompilerSettings.ADDITIONAL_OPTIONS_OVERRIDE.clear();
    myCompilerSettings.ADDITIONAL_OPTIONS_OVERRIDE.putAll(myOptionsOverride.getModuleOptionsMap());
  }

  @Override
  public void reset() {
    myCbDeprecation.setSelected(myCompilerSettings.DEPRECATION);
    myCbDebuggingInfo.setSelected(myCompilerSettings.DEBUGGING_INFO);
    myCbGenerateNoWarnings.setSelected(myCompilerSettings.GENERATE_NO_WARNINGS);
    myCbProceedOnErrors.setSelected(myCompilerSettings.PROCEED_ON_ERROR);
    myPathToEcjField.setText(FileUtil.toSystemDependentName(myCompilerSettings.ECJ_TOOL_PATH));
    myAdditionalOptionsField.setText(myCompilerSettings.ADDITIONAL_OPTIONS_STRING);
    myOptionsOverride.setModuleOptionsMap(myCompilerSettings.ADDITIONAL_OPTIONS_OVERRIDE);
  }
}
