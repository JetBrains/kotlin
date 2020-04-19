// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory;
import com.intellij.openapi.roots.ui.configuration.UnknownSdk;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.ui.EditorNotificationPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UnknownInvalidSdk implements UnknownSdk {
  private static final Logger LOG = Logger.getInstance(UnknownInvalidSdk.class);

  @NotNull final Sdk mySdk;
  @NotNull final SdkType mySdkType;
  @Nullable UnknownSdkLocalSdkFix myLocalSdkFix = null;
  @Nullable UnknownSdkDownloadableSdkFix myDownloadableSdkFix = null;

  UnknownInvalidSdk(@NotNull Sdk sdk, @NotNull SdkType sdkType) {
    mySdk = sdk;
    mySdkType = sdkType;
  }

  @NotNull
  @Override
  public SdkType getSdkType() {
    return mySdkType;
  }

  @Override
  @NotNull
  public String getSdkName() {
    return mySdk.getName();
  }

  @Override
  @Nullable
  public String getExpectedVersionString() {
    return mySdk.getVersionString();
  }

  void applyLocalFix(@NotNull Project project) {
    if (myLocalSdkFix == null) return;

    String sdkFixVersionString = myLocalSdkFix.getVersionString();
    String sdkHome = myLocalSdkFix.getExistingSdkHome();

    copySdk(project, sdkFixVersionString, sdkHome);
  }

  private void copySdk(@NotNull Project project,
                       @NotNull String sdkFixVersionString,
                       @NotNull String sdkHome) {
    WriteAction.run(() -> {
      SdkModificator mod = mySdk.getSdkModificator();
      mod.setVersionString(sdkFixVersionString);
      mod.setHomePath(sdkHome);
      mod.commitChanges();

      mySdkType.setupSdkPaths(mySdk);
    });

    UnknownSdkTracker.getInstance(project).updateUnknownSdksNow();
  }

  void applyDownloadFix(@NotNull Project project) {
    if (myDownloadableSdkFix == null) return;

    UnknownSdkDownloader.downloadFix(project,
                                     this,
                                     myDownloadableSdkFix,
                                     __ -> mySdk,
                                     __ -> {
                                     },
                                     sdk -> {
                                       UnknownSdkTracker.getInstance(project).updateUnknownSdksNow();
                                     }
    );
  }

  @NotNull
  public EditorNotificationPanel.ActionHandler createSdkSelectionPopup(@NotNull Project project) {
    String sdkName = mySdk.getName();
    return SdkPopupFactory
      .newBuilder()
      .withProject(project)
      //filter the same-named SDK from the list is needed for invalid sdk case
      .withSdkFilter(sdk -> !Objects.equals(sdk.getName(), sdkName))
      .withSdkTypeFilter(type -> Objects.equals(type, mySdkType))
      .onSdkSelected(sdk -> {
        String homePath = sdk.getHomePath();
        String versionString = sdk.getVersionString();
        if (homePath != null && versionString != null) {
          copySdk(project, versionString, homePath);
        } else {
          LOG.warn("Newly added SDK has invalid home or version: " + sdk + ", home=" + homePath + " version=" + versionString);
        }
      })
      .buildEditorNotificationPanelHandler();
  }

  static void removeAndUpdate(@NotNull List<UnknownInvalidSdk> invalidSdks,
                              @NotNull List<UnknownSdk> fixable,
                              @NotNull Map<UnknownSdk, UnknownSdkLocalSdkFix> localFixes,
                              @NotNull Map<UnknownSdk, UnknownSdkDownloadableSdkFix> downloadFixes) {
    fixable.removeAll(invalidSdks);
    for (UnknownInvalidSdk invalidSdk : invalidSdks) {
      invalidSdk.myLocalSdkFix = localFixes.remove(invalidSdk);
      invalidSdk.myDownloadableSdkFix = downloadFixes.remove(invalidSdk);
    }
  }

  @NotNull
  static List<UnknownInvalidSdk> resolveInvalidSdks(@NotNull List<Sdk> usedSdks) {
    List<UnknownInvalidSdk> result = new ArrayList<>();
    for (Sdk sdk : usedSdks) {
      if (SdkDownloadTracker.getInstance().isDownloading(sdk)) continue;

      UnknownInvalidSdk invalidSdk = resolveInvalidSdk(sdk);
      if (invalidSdk != null) {
        result.add(invalidSdk);
      }
    }
    return result;
  }

  @Nullable
  private static UnknownInvalidSdk resolveInvalidSdk(@NotNull Sdk sdk) {
    SdkTypeId type = sdk.getSdkType();
    if (!(type instanceof SdkType)) return null;
    SdkType sdkType = (SdkType)type;

    //for tests
    //noinspection TestOnlyProblems
    if (ApplicationManager.getApplication().isUnitTestMode() && sdk instanceof MockSdk) {
      return null;
    }

    try {
      String homePath = sdk.getHomePath();
      if (homePath != null && sdkType.isValidSdkHome(homePath)) {
        return null;
      }
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.warn("Failed to validate SDK " + sdk + ". " + e.getMessage(), e);
      return null;
    }

    return new UnknownInvalidSdk(sdk, sdkType);
  }
}
