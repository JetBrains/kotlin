// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.formatting.fileSet.FileSetDescriptor;
import com.intellij.formatting.fileSet.NamedScopeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ExcludedFilesScopeDialog extends ExcludedFilesDialogBase {
  private ExcludedFilesScopeForm myForm;
  private DefaultComboBoxModel<String> myScopeListModel;

  public final static int EDIT_SCOPES = NEXT_USER_EXIT_CODE;

  private final Action myEditAction;
  private final List<NamedScope> myAvailableScopes;

  protected ExcludedFilesScopeDialog(@NotNull Project project,
                                     @NotNull List<NamedScope> availableScopes) {
    super(project);
    myAvailableScopes = availableScopes;
    setTitle("Add Scope");
    myEditAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        close(EDIT_SCOPES);
      }
    };
    myEditAction.putValue(Action.NAME, "Edit Scopes...");
    init();
    fillScopesList(availableScopes);
  }


  private void fillScopesList(@NotNull List<NamedScope> availableScopes) {
    myScopeListModel = new DefaultComboBoxModel<>();
    for (NamedScope scope : availableScopes) {
      myScopeListModel.addElement(scope.getName());
    }
    myForm.getScopesList().setModel(myScopeListModel);
  }


  @Nullable
  @Override
  public FileSetDescriptor getDescriptor() {
    int selectedIndex = myForm.getScopesList().getSelectedIndex();
    String scopeName = selectedIndex >= 0 ? myScopeListModel.getElementAt(selectedIndex) : null;
    if (scopeName != null) {
      for (NamedScope scope : myAvailableScopes) {
        if (scopeName.equals(scope.getName())) {
          return new NamedScopeDescriptor(scope);
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    myForm = new ExcludedFilesScopeForm();
    return myForm.getTopPanel();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[] {getOKAction(), getCancelAction(), myEditAction};
  }

}
