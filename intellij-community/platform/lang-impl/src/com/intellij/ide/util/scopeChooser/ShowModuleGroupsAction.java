/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.util.scopeChooser;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.packageDependencies.DependencyUISettings;
import org.jetbrains.annotations.NotNull;

public class ShowModuleGroupsAction extends ToggleAction {
  private final Runnable myUpdate;

  public ShowModuleGroupsAction(final Runnable update) {
    super("Show Module Groups",
          "Show/hide module groups", AllIcons.Actions.GroupByModuleGroup);
    myUpdate = update;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent event) {
    return DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent event, boolean flag) {
    DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS = flag;
    myUpdate.run();
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(DependencyUISettings.getInstance().UI_SHOW_MODULES);
  }
}
