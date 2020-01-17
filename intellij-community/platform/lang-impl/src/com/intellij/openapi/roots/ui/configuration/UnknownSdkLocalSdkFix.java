// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import org.jetbrains.annotations.NotNull;

/**
 * Locally detected SDK to fix immediately
 */
public interface UnknownSdkLocalSdkFix {
  /**
   * @return the resolved home of the detected SDK to configure
   */
  @NotNull
  String getExistingSdkHome();

  /**
   * @return the actual version string of the SDK
   */
  @NotNull String getVersionString();

  /**
   * @return suggested name for an SDK to be created, still, the name could
   * be altered to avoid conflicts
   */
  @NotNull
  String getSuggestedSdkName();
}
