// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTracker;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SdkTrackerImpl extends SdkTracker {
  @Override
  public boolean isSdkReady(@NotNull Sdk sdk) {
    return !SdkDownloadTracker.getInstance().isDownloading(sdk);
  }

  @Override
  public void whenReady(@NotNull Sdk sdk,
                        @NotNull Disposable lifetime,
                        @NotNull Consumer<Sdk> onReady) {
    ApplicationManager.getApplication().invokeLater(() -> {
      SdkDownloadTracker.getInstance().tryRegisterDownloadingListener(sdk, lifetime, new AbstractProgressIndicatorExBase(), __ -> {
        AppExecutorUtil.getAppExecutorService().execute(() -> onReady.accept(sdk));
      });
    });
  }
}
