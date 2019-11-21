// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public interface FavoritesListener {
  default void rootsChanged() {
  }

  void listAdded(@NotNull String listName);

  void listRemoved(@NotNull String listName);
}
