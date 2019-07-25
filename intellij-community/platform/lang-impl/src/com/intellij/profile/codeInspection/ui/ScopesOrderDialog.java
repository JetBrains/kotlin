// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.scopeChooser.ScopeChooserConfigurable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.search.scope.NonProjectFilesScope;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScopesOrderDialog extends DialogWrapper {
  private final JList<String> myOptionsList = new JBList<>();
  private final InspectionProfileImpl myInspectionProfile;
  @NotNull
  private final Project myProject;
  private final JPanel myPanel;
  private final MyModel myModel;

  ScopesOrderDialog(@NotNull final Component parent,
                    @NotNull InspectionProfileImpl inspectionProfile,
                    @NotNull Project project) {
    super(parent, true);
    myInspectionProfile = inspectionProfile;
    myProject = project;
    myModel = new MyModel();
    reloadScopeList();
    myOptionsList.setModel(myModel);
    myOptionsList.setSelectedIndex(0);

    final JPanel listPanel = ToolbarDecorator.createDecorator(myOptionsList).setMoveDownAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        ListUtil.moveSelectedItemsDown(myOptionsList);
      }
    }).setMoveUpAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        ListUtil.moveSelectedItemsUp(myOptionsList);
      }
    }).addExtraAction(new AnActionButton("Edit Scopes", AllIcons.Actions.Edit) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        ShowSettingsUtil.getInstance().editConfigurable(project, new ScopeChooserConfigurable(project));
        reloadScopeList();
      }
    }).disableRemoveAction().disableAddAction().createPanel();
    final JLabel descr = new JLabel("<html><p>If file appears in two or more scopes, it will be " +
                                           "inspected with settings of the topmost scope in list above.</p><p/>" +
                                           "<p>Scope order is set globally for all inspections in the profile.</p></html>");
    UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, descr);
    myPanel = new JPanel();
    myPanel.setLayout(new BorderLayout());
    myPanel.add(listPanel, BorderLayout.CENTER);
    myPanel.add(descr, BorderLayout.SOUTH);
    init();
    setTitle("Scopes Order");
  }

  private void reloadScopeList() {
    myModel.removeAllElements();

    final List<String> scopes = new ArrayList<>();
    for (final NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(myProject)) {
      for (final NamedScope scope : holder.getScopes()) {
        if (!(scope instanceof NonProjectFilesScope)) {
          scopes.add(scope.getName());
        }
      }
    }
    scopes.remove(CustomScopesProviderEx.getAllScope().getName());
    Collections.sort(scopes, new ScopeOrderComparator(myInspectionProfile));
    for (String scopeName : scopes) {
      myModel.addElement(scopeName);
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  protected void doOKAction() {
    final int size = myOptionsList.getModel().getSize();
    final String[] newScopeOrder = new String[size];
    for (int i = 0; i < size; i++) {
      final String scopeName = myOptionsList.getModel().getElementAt(i);
      newScopeOrder[i] = scopeName;
    }
    if (!Arrays.equals(newScopeOrder, myInspectionProfile.getScopesOrder())) {
      myInspectionProfile.setScopesOrder(newScopeOrder);
    }
    super.doOKAction();
  }

  private static class MyModel extends DefaultListModel<String> implements EditableModel {
    @Override
    public void addRow() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
      String scope1 = getElementAt(newIndex);
      set(newIndex, getElementAt(oldIndex));
      set(oldIndex, scope1);
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
      return true;
    }

    @Override
    public void removeRow(int idx) {
      throw new UnsupportedOperationException();
    }
  }
}
