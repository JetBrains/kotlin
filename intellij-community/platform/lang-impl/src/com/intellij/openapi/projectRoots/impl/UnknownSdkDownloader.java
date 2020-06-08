// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.UnknownSdk;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ApiStatus.Internal
public final class UnknownSdkDownloader {
  private static final Logger LOG = Logger.getInstance(UnknownSdkDownloader.class);

  @ApiStatus.Internal
  public static void downloadFix(@Nullable Project project,
                                 @NotNull UnknownSdk info,
                                 @NotNull UnknownSdkDownloadableSdkFix fix,
                                 @NotNull Function<SdkDownloadTask, Sdk> createSdk,
                                 @NotNull Consumer<? super Sdk> onSdkNameReady,
                                 @NotNull Consumer<? super Sdk> onCompleted) {
    SdkDownloadTask task;
    String title = ProjectBundle.message("progress.title.downloading.sdk");
    try {
      task = ProgressManager.getInstance().run(new Task.WithResult<SdkDownloadTask, RuntimeException>(project, title, true) {
        @Override
        protected SdkDownloadTask compute(@NotNull ProgressIndicator indicator) {
          return fix.createTask(indicator);
        }
      });
    } catch (ProcessCanceledException e) {
      onCompleted.consume(null);
      throw e;
    } catch (Exception error) {
      LOG.warn("Failed to download " + info.getSdkType().getPresentableName() + " " + fix.getDownloadDescription() + " for " + info + ". " + error.getMessage(), error);
      ApplicationManager.getApplication().invokeLater(() -> {
        Messages.showErrorDialog(ProjectBundle.message("dialog.message.failed.to.download.0.1", fix.getDownloadDescription(),
                                                       error.getMessage()), title);
      });
      onCompleted.consume(null);
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        Disposable lifetime = Disposer.newDisposable();
        Sdk sdk = createSdk.apply(task);
        SdkDownloadTracker downloadTracker = SdkDownloadTracker.getInstance();
        downloadTracker.registerSdkDownload(sdk, task);
        downloadTracker.tryRegisterDownloadingListener(sdk, lifetime, new ProgressIndicatorBase(), success -> {
          Disposer.dispose(lifetime);
          onCompleted.consume(success ? sdk : null);
        });

        onSdkNameReady.consume(sdk);
        downloadTracker.startSdkDownloadIfNeeded(sdk);
      } catch (Exception error) {
        LOG.warn("Failed to download " + info.getSdkType().getPresentableName() + " " + fix.getDownloadDescription() + " for " + info + ". " + error.getMessage(), error);
        ApplicationManager.getApplication().invokeLater(() -> {
          Messages.showErrorDialog(
            ProjectBundle.message("dialog.message.failed.to.download.0.1", fix.getDownloadDescription(), error.getMessage()), title);
        });
        onCompleted.consume(null);
      }
    });
  }
}
