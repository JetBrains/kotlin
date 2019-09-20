/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.compiler.impl.javaCompiler.javac;

import com.intellij.compiler.impl.javaCompiler.CompilerModuleOptionsComponent;
import com.intellij.compiler.options.ComparingUtils;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 */
public class JavacConfigurable implements Configurable{
  private JPanel myPanel;
  private JBCheckBox myCbPreferTargetJdkCompiler;
  private JCheckBox myCbDebuggingInfo;
  private JCheckBox myCbDeprecation;
  private JCheckBox myCbGenerateNoWarnings;
  private RawCommandLineEditor myAdditionalOptionsField;
  private CompilerModuleOptionsComponent myOptionsOverride;
  private final Project myProject;
  private final JpsJavaCompilerOptions myJavacSettings;

  public JavacConfigurable(Project project, final JpsJavaCompilerOptions javacSettings) {
    myProject = project;
    myJavacSettings = javacSettings;
    myAdditionalOptionsField.setDialogCaption(CompilerBundle.message("java.compiler.option.additional.command.line.parameters"));
    myAdditionalOptionsField.setDescriptor(null, false);
  }

  private void createUIComponents() {
    myOptionsOverride = new CompilerModuleOptionsComponent(myProject);
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    boolean isModified = false;
    isModified |= ComparingUtils.isModified(myCbPreferTargetJdkCompiler, myJavacSettings.PREFER_TARGET_JDK_COMPILER);
    isModified |= ComparingUtils.isModified(myCbDeprecation, myJavacSettings.DEPRECATION);
    isModified |= ComparingUtils.isModified(myCbDebuggingInfo, myJavacSettings.DEBUGGING_INFO);
    isModified |= ComparingUtils.isModified(myCbGenerateNoWarnings, myJavacSettings.GENERATE_NO_WARNINGS);
    isModified |= ComparingUtils.isModified(myAdditionalOptionsField, myJavacSettings.ADDITIONAL_OPTIONS_STRING);
    isModified |= !myOptionsOverride.getModuleOptionsMap().equals(myJavacSettings.ADDITIONAL_OPTIONS_OVERRIDE);

    return isModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    myJavacSettings.PREFER_TARGET_JDK_COMPILER =  myCbPreferTargetJdkCompiler.isSelected();
    myJavacSettings.DEPRECATION =  myCbDeprecation.isSelected();
    myJavacSettings.DEBUGGING_INFO = myCbDebuggingInfo.isSelected();
    myJavacSettings.GENERATE_NO_WARNINGS = myCbGenerateNoWarnings.isSelected();
    myJavacSettings.ADDITIONAL_OPTIONS_STRING = myAdditionalOptionsField.getText();
    myJavacSettings.ADDITIONAL_OPTIONS_OVERRIDE.clear();
    myJavacSettings.ADDITIONAL_OPTIONS_OVERRIDE.putAll(myOptionsOverride.getModuleOptionsMap());
  }

  @Override
  public void reset() {
    myCbPreferTargetJdkCompiler.setSelected(myJavacSettings.PREFER_TARGET_JDK_COMPILER);
    myCbDeprecation.setSelected(myJavacSettings.DEPRECATION);
    myCbDebuggingInfo.setSelected(myJavacSettings.DEBUGGING_INFO);
    myCbGenerateNoWarnings.setSelected(myJavacSettings.GENERATE_NO_WARNINGS);
    myAdditionalOptionsField.setText(myJavacSettings.ADDITIONAL_OPTIONS_STRING);
    myOptionsOverride.setModuleOptionsMap(myJavacSettings.ADDITIONAL_OPTIONS_OVERRIDE);
  }
}
