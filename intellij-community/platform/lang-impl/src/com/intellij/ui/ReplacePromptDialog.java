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

package com.intellij.ui;

import com.intellij.find.FindManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ReplacePromptDialog extends DialogWrapper {
  private final boolean myIsMultiple;
  @Nullable private final FindManager.MalformedReplacementStringException myException;

  public ReplacePromptDialog(boolean isMultipleFiles, String title, Project project) {
    this(isMultipleFiles, title, project, null);
  }

  public ReplacePromptDialog(boolean isMultipleFiles, String title, Project project, @Nullable FindManager.MalformedReplacementStringException exception) {
    super(project, true);
    myIsMultiple = isMultipleFiles;
    myException = exception;
    setButtonsAlignment(SwingConstants.CENTER);
    setTitle(title);
    init();
  }

  @NotNull
  @Override
  protected Action[] createActions(){
    DoAction replaceAction = new DoAction(UIBundle.message("replace.prompt.replace.button"), FindManager.PromptResult.OK);
    replaceAction.putValue(DEFAULT_ACTION,Boolean.TRUE);
    if (myException == null) {
      if (myIsMultiple){
        setCancelButtonText(UIBundle.message("replace.prompt.review.action"));
        return new Action[]{
          replaceAction,
          createSkipAction(),
          new DoAction(UIBundle.message("replace.prompt.all.in.this.file.button"), FindManager.PromptResult.ALL_IN_THIS_FILE),
          new DoAction(UIBundle.message("replace.prompt.skip.all.in.file.button"), FindManager.PromptResult.SKIP_ALL_IN_THIS_FILE),
          new DoAction(UIBundle.message("replace.prompt.all.files.action"), FindManager.PromptResult.ALL_FILES),
          getCancelAction()
        };
      }
      else {
        return new Action[]{
          replaceAction,
          createSkipAction(),
          new DoAction(UIBundle.message("replace.prompt.all.button"), FindManager.PromptResult.ALL),
          getCancelAction()
        };
      }
    } else {
      return new Action[] {
        createSkipAction(),
        getCancelAction()
      };
    }
  }

  private DoAction createSkipAction() {
    return new DoAction(UIBundle.message("replace.prompt.skip.button"), FindManager.PromptResult.SKIP);
  }

  @Override
  public JComponent createNorthPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    Icon icon = Messages.getQuestionIcon();
    JLabel iconLabel = new JLabel(icon);
    panel.add(iconLabel, BorderLayout.WEST);
    JLabel label = new JLabel(getMessage());
    label.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
    label.setForeground(JBColor.foreground());
    panel.add(label, BorderLayout.CENTER);
    return panel;
  }

  protected String getMessage() {
    return myException == null ? UIBundle.message("replace.prompt.replace.occurrence.label") : myException.getMessage();
  }

  @Override
  public JComponent createCenterPanel() {
    return null;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "ReplaceDuplicatesPrompt";
  }

  private class DoAction extends AbstractAction {
    private final int myExitCode;

    DoAction(String name,int exitCode) {
      putValue(Action.NAME, name);
      myExitCode = exitCode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      close(myExitCode);
    }
  }
}

