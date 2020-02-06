// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import org.jetbrains.annotations.NotNull;

/**
 * A download suggestion to fix a missing SDK by downloading it
 * @see com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownload
 */
public interface UnknownSdkDownloadableSdkFix {
  /** User visible description of the proposed download **/
  @NotNull
  String getDownloadDescription();

  /**
   * @return it is helpful for the UI to know the version of the SDK
   * beforehand (e.g. while the SDK is downloading)
   */
  @NotNull
  String getVersionString();

  /**
   * Creates and SDK download task to apply the fix.
   * @see com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownload
   * @see com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
   */
  @NotNull
  SdkDownloadTask createTask(@NotNull ProgressIndicator indicator);
}
