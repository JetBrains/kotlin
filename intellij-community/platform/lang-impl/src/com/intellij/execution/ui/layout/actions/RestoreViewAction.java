/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.layout.CellTransform;
import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RestoreViewAction extends DumbAwareAction {

  private final Content myContent;
  private final CellTransform.Restore myRestoreAction;

  private boolean myAlert;

  public RestoreViewAction(final Content content, CellTransform.Restore restore) {
    myContent = content;
    myRestoreAction = restore;
    myContent.addPropertyChangeListener(l -> {
      if (Content.PROP_ALERT.equals(l.getPropertyName())) {
        myAlert = true;
      }
    });
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    Presentation p = e.getPresentation();
    p.setText(ActionsBundle.message("action.Runner.RestoreView.text", myContent.getDisplayName()));
    p.setDescription(ActionsBundle.message("action.Runner.RestoreView.description"));
    Icon icon = myContent.getIcon();
    if (myAlert) {
      icon = new LayeredIcon(icon, AllIcons.Nodes.TabAlert);
    }
    p.setIcon(icon == null ? AllIcons.Debugger.RestoreLayout : icon);
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    myRestoreAction.restoreInGrid();
  }

  public Content getContent() {
    return myContent;
  }
}
