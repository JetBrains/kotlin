// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkTypeId;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Extension point to provide a custom UI to allow a user to
 * select an SDK from a list and have the implementation do download it
 */
public interface SdkDownload {
  @ApiStatus.Experimental
  ExtensionPointName<SdkDownload> EP_NAME = ExtensionPointName.create("com.intellij.sdkDownload");

  /**
   * Returns {@code true} if the extension supports the given SdkType, and other
   * methods from that interface could be called;
   * {@code false} otherwise
   */
  boolean supportsDownload(@NotNull SdkTypeId sdkTypeId);

  /**
   * @return the icon to show for the download action in the dialog
   * for the given {@param sdkTypeId}, which satisfies the
   * {@link #supportsDownload(SdkTypeId)} test
   * Invoked in the EDT thread
   */
  @NotNull
  default Icon getIconForDownloadAction(@NotNull SdkTypeId sdkTypeId) {
    return AllIcons.Actions.Download;
  }

  /**
   * Shows the custom SDK download UI based on selected SDK in parent component. The implementation should do the
   * {@param callback} with an information on the new SDK via {@link SdkDownloadTask} instance.
   *
   * @param sdkTypeId          the same {@param sdkTypeId} as was used in the {@link #supportsDownload(SdkTypeId)}
   * @param sdkModel           the list of SDKs currently displayed in the configuration dialog.
   * @param parentComponent    the parent component for showing the dialog.
   * @param selectedSdk        current selected sdk in parentComponent
   * @param sdkCreatedCallback the callback to which the created SDK is passed.
   *
   * @see #supportsDownload(SdkTypeId)
   * @see SdkDownloadTask
   */
  void showDownloadUI(@NotNull SdkTypeId sdkTypeId,
                      @NotNull SdkModel sdkModel,
                      @NotNull JComponent parentComponent,
                      @Nullable Sdk selectedSdk,
                      @NotNull Consumer<SdkDownloadTask> sdkCreatedCallback);
}
