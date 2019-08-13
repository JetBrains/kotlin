// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.configuration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.ui.TextAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class BrowseModuleValueActionListener<T extends JComponent> implements ActionListener {
  private final Project myProject;
  private ComponentWithBrowseButton<T> myField;

  protected BrowseModuleValueActionListener(Project project) {
    myProject = project;
  }

  public Project getProject() {
    return myProject;
  }

  public JComponent getField() {
    return myField;
  }

  public void setField(@NotNull ComponentWithBrowseButton<T> field) {
    myField = field;
    myField.addActionListener(this);
    myField.setButtonEnabled(!myProject.isDefault());
  }

  public String getText() {
    return ((TextAccessor)myField).getText();
  }

  public void detach() {
    if (myField != null) {
      myField.removeActionListener(this);
      myField = null;
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String text = showDialog();
    if (text != null) {
      ((TextAccessor)myField).setText(text);
    }
  }

  @Nullable
  protected abstract String showDialog();
}