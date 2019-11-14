// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Extension point to provide a custom UI to allow a user to
 * select a SDK from a list and have the implementation do download it
 */
public interface SdkDownload {
  ExtensionPointName<SdkDownload> EP_NAME = ExtensionPointName.create("com.intellij.sdkDownload");

  /**
   * Returns true if the download is supported for the given SdkType
   */
  boolean supportsDownload(@NotNull SdkTypeId sdkTypeId);

  /**
   * Shows the custom SDK download UI based on selected SDK in parent component. The implementation should do the
   * {@param callback} with a created but probably incomplete {@link Sdk}. The instance should be filled lazily in the
   * background. Implementation should be aware of multiple {@link Sdk} instances for the same logical entity.
   * <br/>
   * Use the {@link SdkModel#createSdk} methods to create an and fill an instance of the {@link Sdk}. The implementations
   * should not add sdk to the jdkTable neither invoke {@link SdkType#setupSdkPaths}.
   *
   * @param sdkTypeId          the same {@param sdkTypeId} as was used in the {@link #supportsDownload(SdkTypeId)}
   * @param sdkModel           the list of SDKs currently displayed in the configuration dialog.
   * @param parentComponent    the parent component for showing the dialog.
   * @param selectedSdk        current selected sdk in parentComponent
   * @param sdkCreatedCallback the callback to which the created SDK is passed.
   *
   * @see #supportsDownload(SdkTypeId)
   */
  void showDownloadUI(@NotNull SdkTypeId sdkTypeId,
                      @NotNull SdkModel sdkModel,
                      @NotNull JComponent parentComponent,
                      @Nullable Sdk selectedSdk,
                      @NotNull Consumer<Sdk> sdkCreatedCallback);
}
