// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface OptionsContainingConfigurable {
  OptionsContainingConfigurable EMPTY = new OptionsContainingConfigurable() {
    @NotNull
    @Override
    public Set<String> processListOptions() {
      return Collections.emptySet();
    }
  };

  @NotNull
  Set<String> processListOptions();

  /**
   * @return A map of paths each having a set of options which belong to it, for e.g. a tab name and options under
   *         the tab.
   */
  default Map<String,Set<String>> processListOptionsWithPaths() {
    return Collections.emptyMap();
  }

}
