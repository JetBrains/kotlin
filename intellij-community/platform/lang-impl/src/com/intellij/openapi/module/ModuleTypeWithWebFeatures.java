/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.openapi.module;

import org.jetbrains.annotations.NotNull;

public interface ModuleTypeWithWebFeatures {
  static boolean isAvailable(@NotNull Module module) {
    ModuleType moduleType = ModuleType.get(module);
    if (!(moduleType instanceof ModuleTypeWithWebFeatures)) return false;

    return ((ModuleTypeWithWebFeatures) moduleType).hasWebFeatures(module);
  }

  boolean hasWebFeatures(@NotNull Module module);
}
