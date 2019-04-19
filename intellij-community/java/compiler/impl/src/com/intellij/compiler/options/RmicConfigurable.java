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
package com.intellij.compiler.options;

import com.intellij.compiler.impl.rmiCompiler.RmicConfiguration;
import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.compiler.RmicCompilerOptions;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author Eugene Zhuravlev
 */
public class RmicConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private JPanel myPanel;
  private JCheckBox myCbEnabled;
  private JCheckBox myCbGenerateIiopStubs;
  private JCheckBox myCbDebuggingInfo;
  private JCheckBox myCbGenerateNoWarnings;
  private RawCommandLineEditor myAdditionalOptionsField;
  private final RmicCompilerOptions myRmicSettings;
  private final Project myProject;
  private JLabel myFieldLabel;

  public RmicConfigurable(final Project project) {
    myRmicSettings = RmicConfiguration.getOptions(project);
    myProject = project;
    myCbEnabled.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        setOptionsEnabled(e.getStateChange() == ItemEvent.SELECTED);
      }
    });
    myAdditionalOptionsField.setDialogCaption(myFieldLabel.getText());
  }

  private void setOptionsEnabled(final boolean selected) {
    myCbGenerateIiopStubs.setEnabled(selected);
    myCbGenerateNoWarnings.setEnabled(selected);
    myCbDebuggingInfo.setEnabled(selected);
    myFieldLabel.setEnabled(selected);
    myAdditionalOptionsField.setEnabled(selected);
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("rmi.compiler.description");
  }

  @Override
  public String getHelpTopic() {
    return "reference.projectsettings.compiler.rmicompiler";
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    boolean isModified = false;
    isModified |= ComparingUtils.isModified(myCbEnabled, myRmicSettings.IS_EANABLED);
    isModified |= ComparingUtils.isModified(myCbGenerateIiopStubs, myRmicSettings.GENERATE_IIOP_STUBS);
    isModified |= ComparingUtils.isModified(myCbDebuggingInfo, myRmicSettings.DEBUGGING_INFO);
    isModified |= ComparingUtils.isModified(myCbGenerateNoWarnings, myRmicSettings.GENERATE_NO_WARNINGS);
    isModified |= ComparingUtils.isModified(myAdditionalOptionsField, myRmicSettings.ADDITIONAL_OPTIONS_STRING);
    return isModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    try {
      myRmicSettings.IS_EANABLED =  myCbEnabled.isSelected();
      myRmicSettings.GENERATE_IIOP_STUBS =  myCbGenerateIiopStubs.isSelected();
      myRmicSettings.DEBUGGING_INFO = myCbDebuggingInfo.isSelected();
      myRmicSettings.GENERATE_NO_WARNINGS = myCbGenerateNoWarnings.isSelected();
      myRmicSettings.ADDITIONAL_OPTIONS_STRING = myAdditionalOptionsField.getText();
    }
    finally {
      BuildManager.getInstance().clearState(myProject);
    }
  }

  @Override
  public void reset() {
    myCbEnabled.setSelected(myRmicSettings.IS_EANABLED);
    setOptionsEnabled(myRmicSettings.IS_EANABLED);
    myCbGenerateIiopStubs.setSelected(myRmicSettings.GENERATE_IIOP_STUBS);
    myCbDebuggingInfo.setSelected(myRmicSettings.DEBUGGING_INFO);
    myCbGenerateNoWarnings.setSelected(myRmicSettings.GENERATE_NO_WARNINGS);
    myAdditionalOptionsField.setText(myRmicSettings.ADDITIONAL_OPTIONS_STRING);
  }
}
