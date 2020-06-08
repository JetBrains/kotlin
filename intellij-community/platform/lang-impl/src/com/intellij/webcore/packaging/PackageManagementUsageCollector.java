// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.webcore.packaging;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PackageManagementUsageCollector {

  private PackageManagementUsageCollector() {}

  public static void triggerBrowseAvailablePackagesPerformed(@NotNull Project project, @Nullable PackageManagementService service) {
    trigger(project, service, "browseAvailablePackages");
  }

  public static void triggerInstallPerformed(@NotNull Project project, @Nullable PackageManagementService service) {
    trigger(project, service, "install");
  }

  public static void triggerUpgradePerformed(@NotNull Project project, @Nullable PackageManagementService service) {
    trigger(project, service, "upgrade");
  }

  public static void triggerUninstallPerformed(@NotNull Project project, @Nullable PackageManagementService service) {
    trigger(project, service, "uninstall");
  }

  private static void trigger(@NotNull Project project, @Nullable PackageManagementService service, @NotNull String actionName) {
    String serviceName = toKnownServiceName(service);
    if (serviceName != null) {
      FeatureUsageData data = new FeatureUsageData().addData("service", serviceName);
      FUCounterUsageLogger.getInstance().logEvent(project, "package.management.ui", actionName, data);
    }
  }

  @Nullable
  private static String toKnownServiceName(@Nullable PackageManagementService service) {
    if (service == null) return null;
    PluginInfo info = PluginInfoDetectorKt.getPluginInfo(service.getClass());
    return info.isSafeToReport() ? service.getID() : null;
  }
}
