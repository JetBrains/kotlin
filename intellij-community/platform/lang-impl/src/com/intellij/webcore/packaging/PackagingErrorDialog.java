/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.webcore.packaging;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author vlan
 */
public class PackagingErrorDialog extends DialogWrapper {
  private JPanel myMainPanel;
  private JTextArea myCommandOutput;
  private JPanel myCommandOutputPanel;
  private JPanel myCommandPanel;
  private JTextPane myCommand;
  private JPanel mySolutionPanel;
  private JTextPane mySolution;
  private JPanel myDetailsPanel;
  private JTextArea myDetails;
  private JPanel myMessagePanel;
  private JBLabel myDetailsLabel;
  private JTextPane myMessage;
  private JBLabel myMessageIcon;

  public PackagingErrorDialog(@NotNull String title, @NotNull PackageManagementService.ErrorDescription errorDescription) {
    super(false);
    init();
    setResizable(false);
    setTitle(title);
    final String command = errorDescription.getCommand();
    final String output = errorDescription.getOutput();
    final String message = errorDescription.getMessage();
    final String solution = errorDescription.getSolution();

    final boolean extendedInfo = command != null || output != null || solution != null;

    myDetailsPanel.setVisible(!extendedInfo);
    myMessagePanel.setVisible(extendedInfo);
    myCommandPanel.setVisible(command != null);
    myCommandOutputPanel.setVisible(output != null);
    mySolutionPanel.setVisible(solution != null);

    if (extendedInfo) {
      myMessage.setText(message);
      myMessageIcon.setIcon(Messages.getErrorIcon());
    }
    else {
      myDetails.setText(message);
      myDetailsLabel.setIcon(Messages.getErrorIcon());
    }

    if (command != null) {
      myCommand.setText(command);
    }
    if (output != null) {
      myCommandOutput.setText(output);
    }
    if (solution != null) {
      mySolution.setText(solution);
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }
}
