// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy.newAPI;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class HierarchyScopeType {
  private final Supplier<String> myGetName;

  public HierarchyScopeType(@NotNull Supplier<String> getName) {
    myGetName = getName;
  }

  String getName() {
    return myGetName.get();
  }
}