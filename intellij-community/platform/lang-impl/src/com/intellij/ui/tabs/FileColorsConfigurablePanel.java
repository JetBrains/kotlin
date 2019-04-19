/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.ui.tabs;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.scopeChooser.EditScopesDialog;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageType;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
public class FileColorsConfigurablePanel extends JPanel implements Disposable {
  private FileColorManagerImpl myManager;
  private final JCheckBox myEnabledCheckBox;
  private final JCheckBox myTabsEnabledCheckBox;
  private final JCheckBox myProjectViewEnabledCheckBox;
  private final FileColorSettingsTable myLocalTable;
  private final FileColorSettingsTable mySharedTable;

  public FileColorsConfigurablePanel(@NotNull final FileColorManagerImpl manager) {
    setLayout(new BorderLayout());
    myManager = manager;

    final JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

    myEnabledCheckBox = new JCheckBox("Enable File Colors");
    myEnabledCheckBox.setMnemonic('F');
    topPanel.add(myEnabledCheckBox);
    topPanel.add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)));

    myTabsEnabledCheckBox = new JCheckBox("Use in Editor Tabs");
    myTabsEnabledCheckBox.setMnemonic('T');
    topPanel.add(myTabsEnabledCheckBox);
    topPanel.add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)));

    myProjectViewEnabledCheckBox = new JCheckBox("Use in Project View");
    myProjectViewEnabledCheckBox.setMnemonic('P');
    topPanel.add(myProjectViewEnabledCheckBox);

    topPanel.add(Box.createHorizontalGlue());

    add(topPanel, BorderLayout.NORTH);

    final JPanel mainPanel = new JPanel(new GridLayout(2, 1));
    mainPanel.setPreferredSize(JBUI.size(300, 500));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

    final List<FileColorConfiguration> localConfigurations = manager.getApplicationLevelConfigurations();
    myLocalTable = new FileColorSettingsTable(manager, localConfigurations) {
      @Override
      protected void apply(@NotNull List<FileColorConfiguration> configurations) {
        final List<FileColorConfiguration> copied = new ArrayList<>();
        try {
          for (final FileColorConfiguration configuration : configurations) {
            copied.add(configuration.clone());
          }
        } catch (CloneNotSupportedException e) {//
        }
        manager.getModel().setConfigurations(copied, false);
      }
    };

    final JPanel panel = ToolbarDecorator.createDecorator(myLocalTable)
      .addExtraAction(new AnActionButton("Share", AllIcons.Actions.Share) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          share();
        }

        @Override
        public boolean isEnabled() {
          return super.isEnabled() && myLocalTable.getSelectedRow() != -1;
        }
      })
      .createPanel();
    final JPanel localPanel = new JPanel(new BorderLayout());
    localPanel.setBorder(IdeBorderFactory.createTitledBorder("Local colors", false));
    localPanel.add(panel, BorderLayout.CENTER);
    mainPanel.add(localPanel);

    mySharedTable = new FileColorSettingsTable(manager, manager.getProjectLevelConfigurations()) {
      @Override
      protected void apply(@NotNull List<FileColorConfiguration> configurations) {
        final List<FileColorConfiguration> copied = new ArrayList<>();
        for (final FileColorConfiguration configuration : configurations) {
          try {
            copied.add(configuration.clone());
          }
          catch (CloneNotSupportedException e) {
            assert false : "Should not happen!";
          }
        }
        manager.getModel().setConfigurations(copied, true);
      }
    };

    final JPanel sharedPanel = new JPanel(new BorderLayout());
    sharedPanel.setBorder(IdeBorderFactory.createTitledBorder("Shared colors", false));
    final JPanel shared = ToolbarDecorator.createDecorator(mySharedTable)
      .addExtraAction(new AnActionButton("Unshare", AllIcons.Actions.Unshare) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          unshare();
        }

        @Override
        public boolean isEnabled() {
          return super.isEnabled() && mySharedTable.getSelectedRow() != -1;
        }
      })
      .createPanel();

    sharedPanel.add(shared, BorderLayout.CENTER);
    mainPanel.add(sharedPanel);
    add(mainPanel, BorderLayout.CENTER);

    final JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    infoPanel.add(new JLabel("Scopes are processed from top to bottom with Local colors first.",
                                MessageType.INFO.getDefaultIcon(), SwingConstants.LEFT));
    JButton editScopes = new JButton("Manage Scopes...");
    editScopes.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        EditScopesDialog.showDialog(myManager.getProject(), null, true);
      }
    });
    infoPanel.add(editScopes, BorderLayout.EAST);
    add(infoPanel, BorderLayout.SOUTH);

    myLocalTable.getEmptyText().setText("No local colors");
    mySharedTable.getEmptyText().setText("No shared colors");
  }

  private void unshare() {
    final int rowCount = mySharedTable.getSelectedRowCount();
    if (rowCount > 0) {
      final int[] rows = mySharedTable.getSelectedRows();
      for (int i = rows.length - 1; i >= 0; i--) {
        FileColorConfiguration removed = mySharedTable.removeConfiguration(rows[i]);
        if (removed != null) {
          myLocalTable.addConfiguration(removed);
        }
      }
    }
  }

  private void share() {
    final int rowCount = myLocalTable.getSelectedRowCount();
    if (rowCount > 0) {
      final int[] rows = myLocalTable.getSelectedRows();
      for (int i = rows.length - 1; i >= 0; i--) {
        FileColorConfiguration removed = myLocalTable.removeConfiguration(rows[i]);
        if (removed != null) {
          mySharedTable.addConfiguration(removed);
        }
      }
    }
  }

  @Override
  public void dispose() {
    myManager = null;
  }

  public boolean isModified() {
    boolean modified;

    modified = myEnabledCheckBox.isSelected() != myManager.isEnabled();
    modified |= myTabsEnabledCheckBox.isSelected() != myManager.isEnabledForTabs();
    modified |= myProjectViewEnabledCheckBox.isSelected() != myManager.isEnabledForProjectView();
    modified |= myLocalTable.isModified() || mySharedTable.isModified();

    return modified;
  }

  public void apply() {
    myManager.setEnabled(myEnabledCheckBox.isSelected());
    myManager.setEnabledForTabs(myTabsEnabledCheckBox.isSelected());
    FileColorManagerImpl.setEnabledForProjectView(myProjectViewEnabledCheckBox.isSelected());

    myLocalTable.apply();
    mySharedTable.apply();

    UISettings.getInstance().fireUISettingsChanged();
  }

  public void reset() {
    myEnabledCheckBox.setSelected(myManager.isEnabled());
    myTabsEnabledCheckBox.setSelected(myManager.isEnabledForTabs());
    myProjectViewEnabledCheckBox.setSelected(myManager.isEnabledForProjectView());

    if (myLocalTable.isModified()) myLocalTable.reset();
    if (mySharedTable.isModified()) mySharedTable.reset();
  }
}
