// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 'Run Anything' popup help section is divided into groups by categories.
 * E.g. 'ruby' help group contains 'ruby' related run configuration commands, 'rvm use #sdk_version' commands etc.
 * <p>
 * To add an own help group extend this class and register {@link #EP_NAME} in your ide or plugin.
 *
 * @param <P>
 */
public abstract class RunAnythingHelpGroup<P extends RunAnythingProvider> extends RunAnythingGroupBase {
  public static final ExtensionPointName<RunAnythingGroup> EP_NAME = ExtensionPointName.create("com.intellij.runAnything.helpGroup");

  /**
   * Returns collections of providers each of them is expecting to provide not null {@link RunAnythingProvider#getHelpItem(DataContext)}
   * See also {@code RunAnythingProviderBase.getHelp*()} methods.
   */
  @NotNull
  public abstract Collection<P> getProviders();

  @NotNull
  @Override
  public Collection<RunAnythingItem> getGroupItems(@NotNull DataContext dataContext, @NotNull String pattern) {
    return getProviders()
      .stream()
      .map(provider -> provider.getHelpItem(dataContext))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
}