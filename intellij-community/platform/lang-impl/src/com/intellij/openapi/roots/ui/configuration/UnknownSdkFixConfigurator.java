// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

public interface UnknownSdkFixConfigurator {
  /**
   * Configures created SDK before it will be added to SdkTable.
   * @param sdk SDK which was created by this fix.
   */
  void configure(@NotNull Sdk sdk);
}