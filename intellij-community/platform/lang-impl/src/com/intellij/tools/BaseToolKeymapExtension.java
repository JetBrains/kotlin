// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.keymap.KeymapExtension;
import com.intellij.openapi.keymap.KeymapGroup;
import com.intellij.openapi.keymap.impl.ui.Group;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;

import java.util.Arrays;
import java.util.HashMap;

public abstract class BaseToolKeymapExtension implements KeymapExtension {

  @Override
  public KeymapGroup createGroup(final Condition<AnAction> filtered, final Project project) {
    final ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
    String[] ids = actionManager.getActionIds(getActionIdPrefix());
    Arrays.sort(ids);
    Group group = new Group(getGroupName(), AllIcons.Nodes.KeymapTools);


    HashMap<String, Group> toolGroupNameToGroup = new HashMap<>();

    for (String id : ids) {
      if (filtered != null && !filtered.value(actionManager.getActionOrStub(id))) continue;
      String groupName = getGroupByActionId(id);

      if (groupName != null && groupName.trim().length() == 0) {
        groupName = null;
      }

      Group subGroup = toolGroupNameToGroup.get(groupName);
      if (subGroup == null) {
        subGroup = new Group(groupName, null, null);
        toolGroupNameToGroup.put(groupName, subGroup);
        if (groupName != null) {
          group.addGroup(subGroup);
        }
      }

      subGroup.addActionId(id);
    }

    Group subGroup = toolGroupNameToGroup.get(null);
    if (subGroup != null) {
      group.addAll(subGroup);
    }

    return group;
  }

  protected abstract String getActionIdPrefix();

  protected abstract String getGroupByActionId(String id);

  protected abstract String getGroupName();
}
