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
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MoveModulesToSubGroupAction extends MoveModulesToGroupAction {
  public MoveModulesToSubGroupAction(ModuleGroup moduleGroup) {
    super(moduleGroup, moduleGroup == null ? IdeBundle.message("action.move.module.new.top.level.group") : IdeBundle.message("action.move.module.to.new.sub.group"));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(e.getData(LangDataKeys.MODULE_CONTEXT_ARRAY) != null);
    String description = IdeBundle.message("action.description.create.new.module.group");
    presentation.setDescription(description);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Module[] modules = e.getRequiredData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    final List<String> newGroup;
    if (myModuleGroup != null) {
      String message = IdeBundle.message("prompt.specify.name.of.module.subgroup", myModuleGroup.presentableText(), whatToMove(modules));
      String subgroup = Messages.showInputDialog(message, IdeBundle.message("title.module.sub.group"), Messages.getQuestionIcon());
      if (subgroup == null || "".equals(subgroup.trim())) return;
      newGroup = ContainerUtil.append(myModuleGroup.getGroupPathList(), subgroup);
    }
    else {
      String message = IdeBundle.message("prompt.specify.module.group.name", whatToMove(modules));
      String group = Messages.showInputDialog(message, IdeBundle.message("title.module.group"), Messages.getQuestionIcon());
      if (group == null || "".equals(group.trim())) return;
      newGroup = Collections.singletonList(group);
    }

    doMove(modules, new ModuleGroup(newGroup), dataContext);
  }
}
