// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleJdkOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectRootManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects usages of all possible SDK references in the project,
 * including both references for a known SDKs (which are from {@link com.intellij.openapi.projectRoots.ProjectJdkTable}
 * and unknown (which are not yet created in the project)
 */
public class SdkUsagesCollector {
  private static final ExtensionPointName<SdkUsagesContributor> EP_NAME = ExtensionPointName.create("com.intellij.sdkUsagesContributor");
  private final Project myProject;

  public SdkUsagesCollector(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  public static SdkUsagesCollector getInstance(@NotNull Project project) {
    return project.getService(SdkUsagesCollector.class);
  }

  public interface SdkUsagesContributor {
    @NotNull
    List<SdkUsage> contributeUsages(@NotNull Project project);
  }

  public static class SdkUsage {
    private final String mySdkName;
    private final String mySdkTypeName;

    public SdkUsage(@NotNull String sdkName, @Nullable String sdkTypeName) {
      mySdkName = sdkName;
      mySdkTypeName = sdkTypeName;
    }

    @NotNull
    public String getSdkName() {
      return mySdkName;
    }

    @Nullable
    public String getSdkTypeName() {
      return mySdkTypeName;
    }
  }

  /**
   * Iterates the project model to detect usages if SDKs
   */
  @NotNull
  public List<SdkUsage> collectSdkUsages() {
    List<SdkUsage> usages = new ArrayList<>();
    EP_NAME.forEachExtensionSafe(it -> usages.addAll(it.contributeUsages(myProject)));
    return usages;
  }

  public static class ProjectSdkUsages implements SdkUsagesContributor {
    @NotNull
    @Override
    public List<SdkUsage> contributeUsages(@NotNull Project project) {
      ProjectRootManager manager = ProjectRootManager.getInstance(project);
      String sdkName = manager.getProjectSdkName();
      String sdkType = manager.getProjectSdkTypeName();

      if (sdkName != null) {
        return Collections.singletonList(new SdkUsage(sdkName, sdkType));
      }
      return Collections.emptyList();
    }
  }

  public static class ModuleSdkUsages implements SdkUsagesContributor {
    @NotNull
    @Override
    public List<SdkUsage> contributeUsages(@NotNull Project project) {
      List<SdkUsage> usages = new ArrayList<>();
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        for (OrderEntry orderEntry : manager.getOrderEntries()) {
          if (orderEntry instanceof ModuleJdkOrderEntry) {
            String jdkName = ((ModuleJdkOrderEntry)orderEntry).getJdkName();
            String jdkType = ((ModuleJdkOrderEntry)orderEntry).getJdkTypeName();
            if (jdkName != null) {
              usages.add(new SdkUsage(jdkName, jdkType));
            }
          }
        }
      }
      return usages;
    }
  }
}
