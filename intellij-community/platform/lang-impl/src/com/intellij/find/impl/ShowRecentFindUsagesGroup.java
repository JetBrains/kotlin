// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.impl;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.usages.ConfigurableUsageTarget;
import com.intellij.usages.impl.UsageViewImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShowRecentFindUsagesGroup extends ActionGroup {
  @Override
  public void update(@NotNull final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabledAndVisible(project != null);
  }

  @Override
  public AnAction @NotNull [] getChildren(@Nullable final AnActionEvent e) {
    if (e == null) return EMPTY_ARRAY;
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null || DumbService.isDumb(project)) return EMPTY_ARRAY;
    final FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
    List<ConfigurableUsageTarget> history = new ArrayList<>(findUsagesManager.getHistory().getAll());
    Collections.reverse(history);

    String description =
      ActionManager.getInstance().getAction(UsageViewImpl.SHOW_RECENT_FIND_USAGES_ACTION_ID).getTemplatePresentation().getDescription();

    List<AnAction> children = new ArrayList<>(history.size());
    for (final ConfigurableUsageTarget usageTarget : history) {
      if (!usageTarget.isValid()) {
        continue;
      }
      String text = usageTarget.getLongDescriptiveName();
      AnAction action = new AnAction(text, description, null) {
        @Override
        public void actionPerformed(@NotNull final AnActionEvent e) {
          findUsagesManager.rerunAndRecallFromHistory(usageTarget);
        }
      };
      children.add(action);
    }
    return children.toArray(AnAction.EMPTY_ARRAY);
  }
}
