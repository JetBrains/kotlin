// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
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
public final class SdkUsagesCollector {
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

  public static final class SdkUsage {
    private final String myUsagePresentableText;
    private final String mySdkName;
    private final String mySdkTypeName;

    public SdkUsage(@NotNull String usagePresentableText,
                    @Nullable String sdkName,
                    @Nullable String sdkTypeName) {
      myUsagePresentableText = usagePresentableText;
      mySdkName = sdkName;
      mySdkTypeName = sdkTypeName;
    }

    /**
     * @return UI friendly description of the SDK-using entity,
     * e.g. Project SDK or Module SDK
     */
    @NotNull
    public String getUsagePresentableText() {
      return myUsagePresentableText;
    }

    @Nullable
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

  public static final class ProjectSdkUsages implements SdkUsagesContributor {
    @NotNull
    @Override
    public List<SdkUsage> contributeUsages(@NotNull Project project) {
      ProjectRootManager manager = ProjectRootManager.getInstance(project);
      String sdkName = manager.getProjectSdkName();
      String sdkType = manager.getProjectSdkTypeName();

      return Collections.singletonList(new SdkUsage("project", sdkName, sdkType));
    }
  }

  public static final class ModuleSdkUsages implements SdkUsagesContributor {
    @NotNull
    @Override
    public List<SdkUsage> contributeUsages(@NotNull Project project) {
      List<SdkUsage> usages = new ArrayList<>();
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        contributeForModule(module, usages);
      }
      return usages;
    }

    private void contributeForModule(@NotNull Module module, @NotNull List<SdkUsage> usages) {
      if (!ModuleTypeId.JAVA_MODULE.equals(module.getModuleTypeName())) return;

      ModuleRootManager manager = ModuleRootManager.getInstance(module);
      String jdkName = null;
      String jdkType = null;

      // we do not track usages from external system provided modules
      if (manager.getExternalSource() != null) {
        return;
      }

      for (OrderEntry orderEntry : manager.getOrderEntries()) {
        if (!(orderEntry instanceof JdkOrderEntry)) continue;

        if (orderEntry instanceof InheritedJdkOrderEntry) return;
        if (orderEntry instanceof ModuleJdkOrderEntry) {
          jdkName = ((ModuleJdkOrderEntry)orderEntry).getJdkName();
          jdkType = ((ModuleJdkOrderEntry)orderEntry).getJdkTypeName();
          continue;
        }

        Logger.getInstance(getClass()).error("Unexpected OrderEntry: " + orderEntry.getClass().getName() + ": " + orderEntry);
      }

      usages.add(new SdkUsage("module \"" + module.getName() + "\"", jdkName, jdkType));
    }
  }
}
