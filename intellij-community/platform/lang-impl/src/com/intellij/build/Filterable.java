// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface Filterable<T> {
  boolean isFilteringEnabled();

  @NotNull
  Predicate<T> getFilter();

  void addFilter(@NotNull Predicate<T> filter);

  void removeFilter(@NotNull Predicate<T> filter);

  boolean contains(@NotNull Predicate<T> filter);
}
