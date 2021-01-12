// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.items;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * {@link RunAnythingItem} represents an item of 'Run Anything' list
 */
public abstract class RunAnythingItem {
  /**
   * Returns text presentation of command
   */
  @NotNull
  public abstract String getCommand();

  /**
   * Creates current item {@link Component}
   *
   * @param isSelected true if item is selected in the list
   * @deprecated use {@link #createComponent(String, Icon, boolean, boolean)}
   */
  @Deprecated
  @Nullable
  public Component createComponent(boolean isSelected) {
    return null;
  }

  /**
   * Creates current item {@link Component}
   *
   * @param pattern    search field input field
   * @param isSelected true if item is selected in the list
   * @param hasFocus   true if item has focus in the list
   */
  @NotNull
  public abstract Component createComponent(@Nullable String pattern, boolean isSelected, boolean hasFocus);
}
