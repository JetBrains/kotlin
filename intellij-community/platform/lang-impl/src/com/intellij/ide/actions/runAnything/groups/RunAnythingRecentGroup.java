// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.RunAnythingCache;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class RunAnythingRecentGroup extends RunAnythingGroupBase {
  public static final RunAnythingRecentGroup INSTANCE = new RunAnythingRecentGroup();

  private RunAnythingRecentGroup() {}

  @NotNull
  @Override
  public String getTitle() {
    return IdeBundle.message("run.anything.recent.group.title");
  }

  @NotNull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    assert project != null;

    Collection<RunAnythingItem> collector = new ArrayList<>();
    for (String command : ContainerUtil.iterateBackward(RunAnythingCache.getInstance(project).getState().getCommands())) {
      for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
        Object matchingValue = provider.findMatchingValue(dataContext, command);
        if (matchingValue != null) {
          //noinspection unchecked
          collector.add(provider.getMainListItem(dataContext, matchingValue));
          break;
        }
      }
    }

    return collector;
  }

  @Override
  protected int getMaxInitialItems() {
    return 10;
  }
}