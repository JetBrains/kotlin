// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.actions.MoveModulesOutsideGroupAction;
import com.intellij.ide.projectView.actions.MoveModulesToSubGroupAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.module.ModuleGrouperKt;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MoveModuleToGroupTopLevel extends ActionGroup {
  @Override
  public void update(@NotNull AnActionEvent e){
    final DataContext dataContext = e.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
    boolean active = project != null && modules != null && modules.length != 0 && !ModuleGrouperKt.isQualifiedModuleNamesEnabled(project);
    e.getPresentation().setVisible(active);
  }

  @Override
  @NotNull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) return EMPTY_ARRAY;

    Project project = getEventProject(e);
    if (project == null) return EMPTY_ARRAY;

    ModifiableModuleModel moduleModel = e.getData(LangDataKeys.MODIFIABLE_MODULE_MODEL);
    ModuleGrouper grouper = ModuleGrouper.instanceFor(project, moduleModel);
    List<String> topLevelGroupNames = new ArrayList<>(getTopLevelGroupNames(grouper));
    Collections.sort(topLevelGroupNames);

    List<AnAction> result = new ArrayList<>();
    result.add(new MoveModulesOutsideGroupAction());
    result.add(new MoveModulesToSubGroupAction(null));
    result.add(Separator.getInstance());
    for (String name : topLevelGroupNames) {
      result.add(new MoveModuleToGroup(new ModuleGroup(Collections.singletonList(name))));
    }
    return result.toArray(AnAction.EMPTY_ARRAY);
  }

  private static Collection<String> getTopLevelGroupNames(ModuleGrouper grouper) {
    Set<String> topLevelGroupNames = new HashSet<>();
    for (final Module child : grouper.getAllModules()) {
      List<String> group = grouper.getGroupPath(child);
      if (!group.isEmpty()) {
        topLevelGroupNames.add(group.get(0));
      }
    }
    return topLevelGroupNames;
  }
}
