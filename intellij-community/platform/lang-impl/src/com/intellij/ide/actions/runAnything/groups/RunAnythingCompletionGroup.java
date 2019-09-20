// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.Matcher;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunAnythingCompletionGroup<V, P extends RunAnythingProvider<V>> extends RunAnythingGroupBase {
  public static final Collection<RunAnythingGroup> MAIN_GROUPS = createCompletionGroups();

  @NotNull private final P myProvider;

  public RunAnythingCompletionGroup(@NotNull P provider) {
    myProvider = provider;
  }

  @NotNull
  protected P getProvider() {
    return myProvider;
  }

  @NotNull
  @Override
  public String getTitle() {
    return ObjectUtils.assertNotNull(getProvider().getCompletionGroupTitle());
  }

  @NotNull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern) {
    P provider = getProvider();
    return ContainerUtil.map(provider.getValues(dataContext, pattern), value -> provider.getMainListItem(dataContext, value));
  }

  @Nullable
  @Override
  protected Matcher getMatcher(@NotNull DataContext dataContext, @NotNull String pattern) {
    return getProvider().getMatcher(dataContext, pattern);
  }

  public static Collection<RunAnythingGroup> createCompletionGroups() {
    return StreamEx.of(RunAnythingProvider.EP_NAME.getExtensions())
                   .map(provider -> createCompletionGroup(provider))
                   .filter(Objects::nonNull)
                   .distinct()
                   .collect(Collectors.toList());
  }

  @Nullable
  public static RunAnythingGroup createCompletionGroup(@NotNull RunAnythingProvider provider) {
    String title = provider.getCompletionGroupTitle();
    if (title == null) {
      return null;
    }

    if (RunAnythingGeneralGroup.GENERAL_GROUP_TITLE.equals(title)) {
      return RunAnythingGeneralGroup.INSTANCE;
    }

    //noinspection unchecked
    return new RunAnythingCompletionGroup(provider);
  }
}