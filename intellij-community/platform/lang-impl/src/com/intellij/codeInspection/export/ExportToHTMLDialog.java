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

package com.intellij.codeInspection.export;

import com.intellij.codeEditor.printing.ExportToHTMLSettings;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.OptionGroup;
import com.intellij.util.ui.GraphicsUtil;

import javax.swing.*;
import java.awt.*;

public class ExportToHTMLDialog extends DialogWrapper{
  private final Project myProject;
  private JCheckBox myCbOpenInBrowser;
  private TextFieldWithBrowseButton myTargetDirectoryField;
  private final boolean myCanBeOpenInBrowser;

  public ExportToHTMLDialog(Project project, final boolean canBeOpenInBrowser) {
    super(project, true);
    myProject = project;
    myCanBeOpenInBrowser = canBeOpenInBrowser;
    setOKButtonText(InspectionsBundle.message("inspection.export.save.button"));
    setTitle(InspectionsBundle.message("inspection.export.dialog.title"));
    init();
  }

  @Override
  protected JComponent createNorthPanel() {
    myTargetDirectoryField = new TextFieldWithBrowseButton();
    return com.intellij.codeEditor.printing.ExportToHTMLDialog.assignLabel(myTargetDirectoryField, myProject);
  }

  @Override
  protected JComponent createCenterPanel() {
    if (!myCanBeOpenInBrowser) return null;
    OptionGroup optionGroup = new OptionGroup();

    addOptions(optionGroup);

    return optionGroup.createPanel();
  }

  protected void addOptions(OptionGroup optionGroup) {
    myCbOpenInBrowser = new JCheckBox();
    myCbOpenInBrowser.setText(InspectionsBundle.message("inspection.export.open.option"));
    optionGroup.add(myCbOpenInBrowser);
  }

  public void reset() {
    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myProject);
    if (myCanBeOpenInBrowser) {
      myCbOpenInBrowser.setSelected(exportToHTMLSettings.OPEN_IN_BROWSER);
    }
    final String text = exportToHTMLSettings.OUTPUT_DIRECTORY;
    myTargetDirectoryField.setText(text);
    if (text != null) {
      myTargetDirectoryField.setPreferredSize(new Dimension(GraphicsUtil.stringWidth(text, myTargetDirectoryField.getFont()) + 100, myTargetDirectoryField.getPreferredSize().height));
    }
  }

  public void apply() {
    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myProject);

    if (myCanBeOpenInBrowser) {
      exportToHTMLSettings.OPEN_IN_BROWSER = myCbOpenInBrowser.isSelected();
    }
    exportToHTMLSettings.OUTPUT_DIRECTORY = myTargetDirectoryField.getText();
  }

  @Override
  protected String getHelpId() {
    return "procedures.inspecting.export";
  }
}
