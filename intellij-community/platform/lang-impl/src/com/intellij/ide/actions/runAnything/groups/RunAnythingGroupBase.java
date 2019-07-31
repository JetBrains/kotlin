// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.text.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public abstract class RunAnythingGroupBase extends RunAnythingGroup {
  @NotNull
  public abstract Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern);

  @Nullable
  protected Matcher getMatcher(@NotNull DataContext dataContext, @NotNull String pattern) {
    return null;
  }

  @Override
  public SearchResult getItems(@NotNull DataContext dataContext,
                               @NotNull DefaultListModel model,
                               @NotNull String pattern,
                               boolean isInsertionMode,
                               @NotNull Runnable cancellationChecker) {
    cancellationChecker.run();
    SearchResult result = new SearchResult();
    for (RunAnythingItem runConfigurationItem : getGroupItems(dataContext, pattern)) {
      Matcher matcher = getMatcher(dataContext, pattern);
      if (matcher == null) {
        matcher = RUN_ANYTHING_MATCHER_BUILDER.fun(pattern).build();
      }
      if (addToList(model, result, runConfigurationItem.getCommand(), isInsertionMode, runConfigurationItem, matcher)) break;
      cancellationChecker.run();
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
  private boolean addToList(@NotNull DefaultListModel model,
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