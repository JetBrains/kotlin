// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.formatting.fileSet.FileSetDescriptor;
import com.intellij.formatting.fileSet.NamedScopeDescriptor;
import com.intellij.ide.util.scopeChooser.EditScopesDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopeManager;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.List;
import java.util.Set;

public class ExcludedFilesList extends JBList<FileSetDescriptor> {

  private final ToolbarDecorator myFileListDecorator;
  private DefaultListModel<FileSetDescriptor> myModel;
  private @Nullable CodeStyleSchemesModel mySchemesModel;

  public ExcludedFilesList() {
    super();
    myFileListDecorator = ToolbarDecorator.createDecorator(this)
      .setAddAction(
        new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            addDescriptor();
          }
        })
      .setRemoveAction(
        new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            removeDescriptor();
          }
        })
      .setEditAction(
        new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            editDescriptor();
          }
        }
      )
      .disableUpDownActions();
    addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        onSelectionChange();
      }
    });
  }

  public void initModel() {
    myModel = createDefaultListModel(new FileSetDescriptor[0]);
    setModel(myModel);
  }

  private void onSelectionChange() {
    int i = getSelectedIndex();
    AnActionButton removeButton = ToolbarDecorator.findRemoveButton(myFileListDecorator.getActionsPanel());
    removeButton.setEnabled(i >= 0);
  }

  public void reset(@NotNull CodeStyleSettings settings) {
    myModel.clear();
    for (FileSetDescriptor descriptor : settings.getExcludedFiles().getDescriptors()) {
      myModel.addElement(descriptor);
    }
  }

  public void apply(@NotNull CodeStyleSettings settings) {
    settings.getExcludedFiles().clear();
    for (int i = 0; i < myModel.getSize(); i++) {
      settings.getExcludedFiles().addDescriptor(myModel.get(i));
    }
  }

  public boolean isModified(@NotNull CodeStyleSettings settings) {
    if (myModel.size() != settings.getExcludedFiles().getDescriptors().size()) return true;
    for (int i = 0; i < myModel.getSize(); i++) {
      if (!myModel.get(i).equals(settings.getExcludedFiles().getDescriptors().get(i))) {
        return true;
      }
    }
    return false;
  }

  public ToolbarDecorator getDecorator() {
    return myFileListDecorator;
  }

  private void addDescriptor() {
    assert mySchemesModel != null;
    List<NamedScope> availableScopes = getAvailableScopes();
    if (!availableScopes.isEmpty()) {
      ExcludedFilesScopeDialog dialog = new ExcludedFilesScopeDialog(mySchemesModel.getProject(), availableScopes);
      dialog.show();
      if (dialog.isOK()) {
        FileSetDescriptor descriptor = dialog.getDescriptor();
        if (descriptor != null) {
          int insertAt = getSelectedIndex() < 0 ? getItemsCount() : getSelectedIndex() + 1;
          int exiting = myModel.indexOf(descriptor);
          if (exiting < 0) {
            myModel.add(insertAt, descriptor);
            setSelectedValue(descriptor, true);
          }
          else {
            setSelectedValue(myModel.get(exiting), true);
          }
        }
      }
      else if (dialog.getExitCode() == ExcludedFilesScopeDialog.EDIT_SCOPES) {
        editScope(null);
      }
    }
    else {
      editScope(null);
    }
  }

  private List<NamedScope> getAvailableScopes() {
    Set<String> usedNames = getUsedScopeNames();
    List<NamedScope> namedScopes = ContainerUtil.newArrayList();
    for (NamedScopesHolder holder : getScopeHolders()) {
      for (NamedScope scope : holder.getEditableScopes()) {
        if (!usedNames.contains(scope.getName())) {
          namedScopes.add(scope);
        }
      }
    }
    return namedScopes;
  }

  private Set<String> getUsedScopeNames() {
    Set<String> usedScopeNames = ContainerUtil.newHashSet();
    for (int i =0 ; i < myModel.size(); i ++) {
      FileSetDescriptor descriptor = myModel.get(i);
      if (descriptor instanceof NamedScopeDescriptor) {
        usedScopeNames.add(descriptor.getName());
      }
    }
    return usedScopeNames;
  }

  private void removeDescriptor() {
    int i = getSelectedIndex();
    if (i >= 0) {
      myModel.remove(i);
    }
  }

  @SuppressWarnings("unused")
  private void editDescriptor() {
    int i = getSelectedIndex();
    FileSetDescriptor selectedDescriptor = i >= 0 ? myModel.get(i) : null;
    if (selectedDescriptor instanceof NamedScopeDescriptor) {
      ensureScopeExists((NamedScopeDescriptor)selectedDescriptor);
      editScope(selectedDescriptor.getName());
    }
    else {
      editScope(null);
    }
  }

  public void setSchemesModel(@NotNull CodeStyleSchemesModel schemesModel) {
    mySchemesModel = schemesModel;
  }

  public void editScope(@Nullable final String selectedName) {
    assert mySchemesModel != null;
    EditScopesDialog scopesDialog = EditScopesDialog.showDialog(getScopeHolderProject(), selectedName);
    if (scopesDialog.isOK()) {
      NamedScope scope = scopesDialog.getSelectedScope();
      if (scope != null) {
        String newName = scope.getName();
        FileSetDescriptor newDesciptor = null;
        if (selectedName == null) {
          newDesciptor = findDescriptor(newName);
          if (newDesciptor == null) {
            newDesciptor = new NamedScopeDescriptor(scope);
            myModel.addElement(newDesciptor);
          }
        }
        else {
          FileSetDescriptor oldDescriptor = findDescriptor(selectedName);
          if (!selectedName.equals(newName)) {
            int index = myModel.indexOf(oldDescriptor);
            myModel.removeElement(oldDescriptor);
            newDesciptor = findDescriptor(newName);
            if (newDesciptor == null) {
              newDesciptor = new NamedScopeDescriptor(scope);
              myModel.add(index, newDesciptor);
            }
          }
          else if (oldDescriptor != null) {
            PackageSet fileSet = scope.getValue();
            oldDescriptor.setPattern(fileSet != null ? fileSet.getText() : null);
          }
        }
        if (newDesciptor != null) {
          setSelectedValue(newDesciptor, true);
        }
      }
    }
  }

  private void ensureScopeExists(@NotNull NamedScopeDescriptor descriptor) {
    List<NamedScopesHolder> holders = getScopeHolders();
    for (NamedScopesHolder holder : holders) {
      if (holder.getScope(descriptor.getName()) != null) return;
    }
    NamedScopesHolder projectScopeHolder = DependencyValidationManager.getInstance(getScopeHolderProject());
    NamedScope newScope = projectScopeHolder.createScope(descriptor.getName(), descriptor.getFileSet());
    projectScopeHolder.addScope(newScope);
  }

  private Project getScopeHolderProject() {
    assert mySchemesModel != null;
    CodeStyleScheme scheme = mySchemesModel.getSelectedScheme();
    return mySchemesModel.isProjectScheme(scheme) ? mySchemesModel.getProject() : ProjectManager.getInstance().getDefaultProject();
  }

  @Nullable
  private FileSetDescriptor findDescriptor(@NotNull String name) {
    for (int i = 0; i < myModel.size(); i++) {
      if (name.equals(myModel.get(i).getName())) return myModel.get(i);
    }
    return null;
  }

  private List<NamedScopesHolder> getScopeHolders() {
    List<NamedScopesHolder> holders = ContainerUtil.newArrayList();
    Project project = getScopeHolderProject();
    holders.add(DependencyValidationManager.getInstance(project));
    holders.add(NamedScopeManager.getInstance(project));
    return holders;
  }
}
