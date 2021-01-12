// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkTypeId;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point to resolve missing SDKs in a project,
 * it is executed from a background thread on project open
 * or after project model changes
 */
public interface UnknownSdkResolver {
  @ApiStatus.Internal
  ExtensionPointName<UnknownSdkResolver> EP_NAME = ExtensionPointName.create("com.intellij.unknownSdkResolver");

  /**
   * Returns {@code true} if the Unknown Sdk features are allowed for the given type,
   * {@code false} otherwise
   */
  boolean supportsResolution(@NotNull SdkTypeId sdkTypeId);

  /**
   * Creates search context. The same object is used to process all unknown SDKs in the project,
   * it a good idea to cache any heavy operations inside that instance.
   * That method must be cheap to run.
   * <br/>
   * Return {@code null} to ignore a given request
   */
  @Nullable
  UnknownSdkLookup createResolver(@Nullable Project project, @NotNull ProgressIndicator indicator);

  interface UnknownSdkLookup {
    /**
     * The implementation should check on a local machine (both in existing SDKs and on the disk)
     * for a possible matching SDK
     *
     * @return best match or {@code null} if there is no fix found
     */
    @Nullable
    UnknownSdkLocalSdkFix proposeLocalFix(@NotNull UnknownSdk sdk, @NotNull ProgressIndicator indicator);

    /**
     * Checks the internet for a possible download to fix the SDK.
     *
     * @return the best possible download for {@link com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker},
     * or {@code null} if there is no fix found
     */
    @Nullable
    UnknownSdkDownloadableSdkFix proposeDownload(@NotNull UnknownSdk sdk, @NotNull ProgressIndicator indicator);
  }
}
