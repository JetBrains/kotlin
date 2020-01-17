// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

public final class ExportToHTMLDialog extends DialogWrapper{
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

  private void addOptions(OptionGroup optionGroup) {
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
