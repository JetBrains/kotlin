// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.roots.ui.configuration.SdkListPresenter;
import com.intellij.openapi.roots.ui.configuration.UnknownSdk;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.intellij.notification.NotificationAction.createSimple;

public class UnknownSdkBalloonNotification {
  private static final NotificationGroup SDK_CONFIGURED_GROUP
    = new NotificationGroup("Missing SDKs", NotificationDisplayType.BALLOON, true);

  @NotNull
  public static UnknownSdkBalloonNotification getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkBalloonNotification.class);
  }

  private final Project myProject;

  public UnknownSdkBalloonNotification(@NotNull Project project) {
    myProject = project;
  }

  public void notifyFixedSdks(@NotNull Map<? extends UnknownSdk, UnknownSdkLocalSdkFix> localFixes) {
    if (localFixes.isEmpty()) return;

    final String title;
    final String change;
    final StringBuilder message = new StringBuilder();

    Set<String> usages = new TreeSet<>();
    for (Map.Entry<? extends UnknownSdk, UnknownSdkLocalSdkFix> entry : localFixes.entrySet()) {
      UnknownSdkLocalSdkFix fix = entry.getValue();
      String usage = "\"" + entry.getKey().getSdkName() + "\"" +
                     " is set to " +
                     fix.getVersionString() +
                     " <br/> " +
                     SdkListPresenter.presentDetectedSdkPath(fix.getExistingSdkHome());
      usages.add(usage);
    }
    message.append(StringUtil.join(usages, "<br/><br/>"));

    if (localFixes.size() == 1) {
      Map.Entry<? extends UnknownSdk, UnknownSdkLocalSdkFix> entry = localFixes.entrySet().iterator().next();
      UnknownSdk info = entry.getKey();
      String sdkTypeName = info.getSdkType().getPresentableName();
      title = sdkTypeName + " is configured";
      change = "Change " + sdkTypeName + "...";
    }
    else {
      title = "SDKs are configured";
      change = "Change SDKs...";
    }

    SDK_CONFIGURED_GROUP.createNotification(title, message.toString(), NotificationType.INFORMATION, null)
      .setImportant(true)
      .addAction(createSimple(
        change,
        () -> ProjectSettingsService.getInstance(myProject).openProjectSettings()))
      .notify(myProject);
  }
}
