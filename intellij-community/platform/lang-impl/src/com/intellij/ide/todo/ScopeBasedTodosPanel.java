/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.ide.todo;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.content.Content;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ScopeBasedTodosPanel extends TodoPanel {
  private static final String SELECTED_SCOPE = "TODO_SCOPE";
  private final Alarm myAlarm;
  private ScopeChooserCombo myScopes;

  public ScopeBasedTodosPanel(final Project project, TodoPanelSettings settings, Content content){
    super(project,settings,false,content);
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    myScopes.getChildComponent().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        rebuildWithAlarm(ScopeBasedTodosPanel.this.myAlarm);
        PropertiesComponent.getInstance(myProject).setValue(SELECTED_SCOPE, myScopes.getSelectedScopeName(), null);
      }
    });
    rebuildWithAlarm(myAlarm);
  }

  @Override
  protected JComponent createCenterComponent() {
    JPanel panel = new JPanel(new BorderLayout());
    final JComponent component = super.createCenterComponent();
    panel.add(component, BorderLayout.CENTER);
    String preselect = PropertiesComponent.getInstance(myProject).getValue(SELECTED_SCOPE);
    myScopes = new ScopeChooserCombo(myProject, false, true, preselect);
    Disposer.register(this, myScopes);
    
    myScopes.setCurrentSelection(false);
    myScopes.setUsageView(false);

    JPanel chooserPanel = new JPanel(new GridBagLayout());
    final JLabel scopesLabel = new JLabel("Scope:");
    scopesLabel.setDisplayedMnemonic('S');
    scopesLabel.setLabelFor(myScopes);
    final GridBagConstraints gc =
      new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                             JBUI.insets(2, 8, 2, 4), 0, 0);
    chooserPanel.add(scopesLabel, gc);
    gc.insets = JBUI.insets(2);
    chooserPanel.add(myScopes, gc);

    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1;
    chooserPanel.add(Box.createHorizontalBox(), gc);
    panel.add(chooserPanel, BorderLayout.NORTH);
    return panel;
  }

  @Override
  protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
    ScopeBasedTodosTreeBuilder builder = new ScopeBasedTodosTreeBuilder(tree, project, myScopes);
    builder.init();
    return builder;
  }
}