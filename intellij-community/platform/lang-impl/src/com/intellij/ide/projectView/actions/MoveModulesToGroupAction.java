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

package com.intellij.ide.projectView.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MoveModulesToGroupAction extends AnAction {
  protected final ModuleGroup myModuleGroup;

  public MoveModulesToGroupAction(ModuleGroup moduleGroup, String title) {
    super(title);
    myModuleGroup = moduleGroup;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    final DataContext dataContext = e.getDataContext();
    final Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
    e.getPresentation().setEnabledAndVisible(modules != null);
    if (modules != null) {
      String description = IdeBundle.message("message.move.modules.to.group", whatToMove(modules), myModuleGroup.presentableText());
      presentation.setDescription(description);
    }
  }

  protected static String whatToMove(@NotNull Module[] modules) {
    return modules.length == 1 ? IdeBundle.message("message.module", modules[0].getName()) : IdeBundle.message("message.modules");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Module[] modules = e.getRequiredData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    doMove(modules, myModuleGroup, e.getDataContext());
  }

  public static void doMove(final @NotNull Module[] modules, final ModuleGroup group, @Nullable final DataContext dataContext) {
    Project project = modules[0].getProject();
    for (final Module module : modules) {
      ModifiableModuleModel model = dataContext != null
                                    ? LangDataKeys.MODIFIABLE_MODULE_MODEL.getData(dataContext)
                                    : null;
      if (model != null){
        model.setModuleGroupPath(module, group == null ? null : group.getGroupPath());
      } else {
        ModuleManagerImpl.getInstanceImpl(project).setModuleGroupPath(module, group == null ? null : group.getGroupPath());
      }
    }

    AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
    if (pane != null) {
      pane.updateFromRoot(true);
    }

    if (!ProjectSettingsService.getInstance(project).processModulesMoved(modules, group) && pane != null) {
      if (group != null) {
        pane.selectModuleGroup(group, true);
      }
      else {
        pane.selectModule(modules[0], true);
      }
    }
  }
}
