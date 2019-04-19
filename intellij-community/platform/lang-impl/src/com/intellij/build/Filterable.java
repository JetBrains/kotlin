// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import java.util.function.Predicate;

public interface Filterable<T> {
  boolean isFilteringEnabled();

  Predicate<T> getFilter();

  void setFilter(Predicate<T> filter);
}
