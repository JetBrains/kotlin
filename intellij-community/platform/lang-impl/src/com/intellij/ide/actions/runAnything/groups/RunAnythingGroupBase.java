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
                               boolean isInsertionMode) {
    ProgressManager.checkCanceled();
    SearchResult result = new SearchResult();
    for (RunAnythingItem item : getGroupItems(dataContext, pattern)) {
      Matcher matcher = getMatcher(dataContext, pattern);
      if (matcher == null) {
        matcher = RUN_ANYTHING_MATCHER_BUILDER.fun(pattern).build();
      }
      if (addToList(model, result, item.getCommand(), isInsertionMode, item, matcher)) break;
      ProgressManager.checkCanceled();
    }

    return result;
  }

  /**
   * Adds limited number of matched items into the list.
   *
   * @param model           needed to avoid adding duplicates into the list
   * @param textToMatch     an item presentation text to be matched with
   * @param isInsertionMode if true gets {@link #getMaxItemsToInsert()} group items, else limits to {@link #getMaxInitialItems()}
   * @param item            a new item that is conditionally added into the model
   * @param matcher         uses for group items filtering
   * @return true if limit exceeded
   */
  private boolean addToList(@NotNull List<RunAnythingItem> model,
                            @NotNull SearchResult result,
                            @NotNull String textToMatch,
                            boolean isInsertionMode,
                            @NotNull RunAnythingItem item,
                            @NotNull Matcher matcher) {
    if (!model.contains(item) && matcher.matches(textToMatch)) {
      if (result.size() == (isInsertionMode ? getMaxItemsToInsert() : getMaxInitialItems())) {
        result.setNeedMore(true);
        return true;
      }
      result.add(item);
    }
    return false;
  }
}