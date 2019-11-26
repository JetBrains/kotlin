// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkTypeId;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Implement that interface and return an instance back via the callback
 * of the {@link SdkDownload#showDownloadUI(SdkTypeId, SdkModel, JComponent, Sdk, Consumer)}
 * method to register an SDK in the model and to initiate a background process
 * of downloading and preparing of it.
 *
 * @see SdkDownload
 */
public interface SdkDownloadTask {
  /**
   * @return suggested name for an SDK to be created, still, the name could
   * be altered to avoid conflicts
   */
  @NotNull
  String getSuggestedSdkName();

  /**
   * @return SDK is expected to have a system dependent home directory.
   * Return the planned canonical directory path where the SDK will be installed
   * by {@link #doDownload(ProgressIndicator)},
   * that path will later be seen from {@link Sdk#getHomePath()}
   */
  @NotNull
  String getPlannedHomeDir();

  /**
   * @return it is helpful for the UI to know the version of the SDK
   * beforehand (e.g. while the SDK is downloading)
   */
  @NotNull
  String getPlannedVersion();

  /**
   * Executes the task in a background thread to download and install
   * a proposed SDK. Once completed, we will set up the SDK the same way
   * as it is installed from a home path.
   * <br/>
   * Should the task fail &mdash; the SDK will be rejected, and the
   * message from the thrown exception will be shown to the user;
   * make sure the exception message is ready to be presented (some details could be
   * still hidden in the {@link Throwable#cause} field)
   * <br/>
   * Implementation should do {@link ProgressIndicator#checkCanceled()} to check
   * for a cancellation request
   */
  void doDownload(@NotNull ProgressIndicator indicator);
}
