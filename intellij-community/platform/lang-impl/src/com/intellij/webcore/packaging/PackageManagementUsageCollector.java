// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.webcore.packaging;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PackageManagementUsageCollector {

  private static final Map<String, String> CLASS_TO_NAME = ContainerUtil.newHashMap(
    Pair.create("com.intellij.javascript.nodejs.settings.NodePackageManagementService", "Node.js"),
    Pair.create("com.jetbrains.python.packaging.ui.PyPackageManagementService", "Python")
  );

  private PackageManagementUsageCollector() {}

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
      FeatureUsageData data = new FeatureUsageData().addData("action", actionName);
      FUCounterUsageLogger.getInstance().logEvent(project, "package.management.ui", serviceName, data);
    }
  }

  @Nullable
  private static String toKnownServiceName(@Nullable PackageManagementService service) {
    return service != null ? CLASS_TO_NAME.get(service.getClass().getName()) : null;
  }
}
