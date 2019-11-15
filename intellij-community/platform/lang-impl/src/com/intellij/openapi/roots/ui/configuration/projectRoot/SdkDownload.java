// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
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
   * Returns true if the download is supported for the given SdkType,
   * if so, the {@link #showDownloadUI(SdkTypeId, SdkModel, JComponent, Sdk, Consumer)}
   * method could be called
   */
  boolean supportsDownload(@NotNull SdkTypeId sdkTypeId);

  /**
   * @return the icon to show for the download action in the UI
   * for the given {@param sdkTypeId}, which satisfies the
   * {@link #supportsDownload(SdkTypeId)} test
   */
  @NotNull
  default Icon getIconForDownloadAction(@NotNull SdkTypeId sdkTypeId) {
    return AllIcons.Actions.Download;
  }

  /**
   * Implement that interface and return an instance back via the callback
   * of the {@link #showDownloadUI(SdkTypeId, SdkModel, JComponent, Sdk, Consumer)}
   * method to register an SDK in the model and to initiate a background process
   * of downloading and preparing of it.
   */
  interface SdkDownloadTask {
    /**
     * @return the suggested name for an SDK to be created, still, the name could
     * be altered to avoid conflicts
     */
    default @Nullable String getSuggestedSdkName() { return null; }

    /**
     * @return SDK is expected to have the a home directory.
     * Return the planned directory where the SDK will be installed
     * by the {@link #getDownloadTask()}
     */
    @NotNull String getPlannedHomeDir();

    /**
     * @return it is helpful for the UI to know the version of the SDK
     * before hand (e.g. while the SDK is downloading)
     */
    @NotNull String getPlannedVersion();

    /**
     * @return the task to execute in order to have the proposed
     * SDK ready. Once completed, we will setup the SDK the same was
     * as it would be installed from the home path via the {@link SdkType}
     * and {@link ProjectSdksModel#setProjectSdk(Sdk)}.
     *
     * Should the task fail - the SDK will be rejected and the
     * message from the thrown exception will be shown to the user,
     * make sure it is ready to be presented (some details could be
     * still hidden in the {@link Throwable#cause} field)
     */
    @NotNull Task.Backgroundable getDownloadTask();
  }



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

  interface DownloadProgressListener {
    @NotNull
    ProgressIndicatorEx getProgressIndicator();

    /**
     * Notifies the UI from the EDT thread that the background download is completed
     * and the related SDK instance is update to reflect the changes.
     */
    void onDownloadCompleted();

    /**
     * Notifies the UI from the EDT thread that the background download has failed
     * with the following error message.
     * //TODO[jo] how should UI handle that failure?
     * //TODO[jo] should we drop such an SDK from the model?
     */
    void onDownloadFailed(@NotNull String message);
  }

  /**
   * Allows to connect the UI (e.g. Project Structure dialog) with the pending SDK download progress.
   * The listener is automatically unsubscribed after either {@link DownloadProgressListener#onDownloadCompleted()}
   * or {@link DownloadProgressListener#onDownloadFailed(String)} callbacks are executed.
   * Multiple attempts to register the same instance of {@link DownloadProgressListener} are merged together
   * without duplication of callbacks
   *
   * @param sdk      the SDK instance to test
   * @param lifetime disposable object for that subscription
   * @param listener the callback interface for the UI
   *
   * @return {@code true} if the given {@param sdk} instance was created by that extension
   *         and there are some planned activities for that {@link Sdk} to make it fully ready;
   *         {@code false} otherwise
   */
  boolean addChangesListener(@NotNull Sdk sdk,
                             @NotNull Disposable lifetime,
                             @NotNull DownloadProgressListener listener);
}
