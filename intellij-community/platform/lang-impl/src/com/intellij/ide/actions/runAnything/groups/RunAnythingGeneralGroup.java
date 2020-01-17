// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class RunAnythingGeneralGroup extends RunAnythingGroupBase {
  @NotNull
  @Override
  public String getTitle() {
    return getGroupTitle();
  }

  @NotNull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern) {
    Collection<RunAnythingItem> collector = new ArrayList<>();

    for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
      if (getGroupTitle().equals(provider.getCompletionGroupTitle())) {
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

  public static String getGroupTitle() {
    return IdeBundle.message("run.anything.general.group.title");
  }
}
