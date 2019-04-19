// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class RunAnythingGeneralGroup extends RunAnythingGroupBase {
  public static final RunAnythingGeneralGroup INSTANCE = new RunAnythingGeneralGroup();
  public static final String GENERAL_GROUP_TITLE = IdeBundle.message("run.anything.general.group.title");

  private RunAnythingGeneralGroup() {}

  @NotNull
  @Override
  public String getTitle() {
    return GENERAL_GROUP_TITLE;
  }

  @NotNull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern) {
    Collection<RunAnythingItem> collector = ContainerUtil.newArrayList();

    for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
      if (GENERAL_GROUP_TITLE.equals(provider.getCompletionGroupTitle())) {
        Collection values = provider.getValues(dataContext, pattern);
        for (Object value : values) {
          //noinspection unchecked
          collector.add(provider.getMainListItem(dataContext, value));
        }
      }
    }

    return collector;
  }

  @Override
  protected int getMaxInitialItems() {
    return 15;
  }
}
