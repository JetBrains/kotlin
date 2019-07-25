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

package com.intellij.application.options.colors;

import com.intellij.ide.DataManager;
import com.intellij.ide.util.scopeChooser.EditScopesDialog;
import com.intellij.ide.util.scopeChooser.ScopeChooserConfigurable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ScopeColorsPageFactory implements ColorAndFontPanelFactory {
  @NotNull
  @Override
  public NewColorAndFontPanel createPanel(@NotNull ColorAndFontOptions options) {
    final JPanel scopePanel = createChooseScopePanel();
    return NewColorAndFontPanel.create(new PreviewPanel.Empty(){
      @Override
      public Component getPanel() {
        return scopePanel;
      }

    }, ColorAndFontOptions.SCOPES_GROUP, options, null, null);
  }

  @NotNull
  @Override
  public String getPanelDisplayName() {
    return ColorAndFontOptions.SCOPES_GROUP;
  }

  private static JPanel createChooseScopePanel() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    JPanel panel = new JPanel(new GridBagLayout());
    //panel.setBorder(new LineBorder(Color.red));
    if (projects.length == 0) return panel;
    GridBagConstraints gc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                   JBUI.emptyInsets(), 0, 0);

    JButton button = new JButton("Manage Scopes...");
    button.setPreferredSize(new Dimension(230, button.getPreferredSize().height));
    panel.add(button, gc);
    gc.gridx = GridBagConstraints.REMAINDER;
    gc.weightx = 1;
    panel.add(new JPanel(), gc);

    gc.gridy++;
    gc.gridx=0;
    gc.weighty = 1;
    panel.add(new JPanel(), gc);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        DataContext context = DataManager.getInstance().getDataContext(panel);
        Settings settings = Settings.KEY.getData(context);
        Project project = CommonDataKeys.PROJECT.getData(context);
        if (settings != null) {
          try {
            if (settings.select(settings.find(ScopeChooserConfigurable.PROJECT_SCOPES)).isRejected()) {
              EditScopesDialog.showDialog(project, null);
            }
          } catch (IllegalStateException ex) {
            EditScopesDialog.showDialog(project, null);
          }
        }
      }
    });
    return panel;
  }
}
