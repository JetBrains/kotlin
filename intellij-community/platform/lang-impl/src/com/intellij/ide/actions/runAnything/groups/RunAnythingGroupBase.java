// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.text.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class RunAnythingGroupBase extends RunAnythingGroup {
  @NotNull
  public abstract Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern);

  @Nullable
  protected Matcher getMatcher(@NotNull DataContext dataContext, @NotNull String pattern) {
    return null;
  }

  @Override
  public SearchResult getItems(@NotNull DataContext dataContext,
                               @NotNull List<RunAnythingItem> model,
                               @NotNull String pattern,
                               int itemsToInsert) {
    ProgressManager.checkCanceled();
    SearchResult result = new SearchResult();
    for (RunAnythingItem item : getGroupItems(dataContext, pattern)) {
      Matcher matcher = getMatcher(dataContext, pattern);
      if (matcher == null) {
        matcher = RUN_ANYTHING_MATCHER_BUILDER.fun(pattern).build();
      }
      if (!model.contains(item) && matcher.matches(item.getCommand())) {
        if (result.size() == itemsToInsert) {
          result.setNeedMore(true);
          break;
        }
        result.add(item);
      }
      ProgressManager.checkCanceled();
    }

    return result;
  }
}