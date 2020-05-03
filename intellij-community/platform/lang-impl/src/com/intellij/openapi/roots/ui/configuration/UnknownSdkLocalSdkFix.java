// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

/**
 * Locally detected SDK to fix immediately
 */
public interface UnknownSdkLocalSdkFix extends UnknownSdkFixConfigurator {
  /**
   * @return the resolved home of the detected SDK to configure
   */
  @NotNull
  String getExistingSdkHome();

  /**
   * @return the actual version string of the SDK,
   * it is used for {@link com.intellij.openapi.projectRoots.SdkModificator#setVersionString(String)}
   * and should be similar to what the respective {@link com.intellij.openapi.projectRoots.SdkType}
   * configures in {@link com.intellij.openapi.projectRoots.SdkType#setupSdkPaths(Sdk)}
   * @see #getPresentableVersionString()
   */
  @NotNull String getVersionString();

  /**
   * @return version string that is short and enough to be shown in UI
   * @see #getVersionString()
   */
  @NotNull
  default String getPresentableVersionString() {
    return getVersionString();
  }

  /**
   * @return suggested name for an SDK to be created, still, the name could
   * be altered to avoid conflicts
   */
  @NotNull
  String getSuggestedSdkName();
}
