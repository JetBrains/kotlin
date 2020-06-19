// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl.ui;

import com.intellij.facet.*;
import com.intellij.facet.ui.FacetDependentToolWindow;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.impl.RegisterToolWindowTaskProvider;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class FacetDependentToolWindowManager implements RegisterToolWindowTaskProvider {
  @Override
  public @NotNull Collection<ToolWindowEP> getTasks(@NotNull Project project) {
    List<FacetDependentToolWindow> facetDependentToolWindows = FacetDependentToolWindow.EXTENSION_POINT_NAME.getExtensionList();
    if (facetDependentToolWindows.isEmpty()) {
      return Collections.emptyList();
    }

    Collection<ToolWindowEP> result = new ArrayList<>();
    ProjectFacetManager projectFacetManager = ProjectFacetManager.getInstance(project);
    l: for (FacetDependentToolWindow extension : facetDependentToolWindows) {
      for (FacetType<?, ?> type : extension.getFacetTypes()) {
        if (projectFacetManager.hasFacets(type.getId())) {
          result.add(extension);
          continue l;
        }
      }
    }
    return result;
  }

  public void projectOpened(@NotNull Project project) {
    ProjectWideFacetListenersRegistry.getInstance(project).registerListener(new ProjectWideFacetAdapter<Facet>() {
      @Override
      public void facetAdded(@NotNull Facet facet) {
        for (FacetDependentToolWindow extension : getDependentExtensions(facet)) {
          ensureToolWindowExists(extension, project);
        }
      }

      @Override
      public void facetRemoved(@NotNull Facet facet) {
        ProjectFacetManager facetManager = ProjectFacetManager.getInstance(project);
        if (facetManager.hasFacets(facet.getTypeId())) {
          return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        for (FacetDependentToolWindow extension : getDependentExtensions(facet)) {
          ToolWindow toolWindow = toolWindowManager.getToolWindow(extension.id);
          if (toolWindow != null) {
            // check for other facets
            for (FacetType<?, ?> facetType : extension.getFacetTypes()) {
              if (facetManager.hasFacets(facetType.getId())) {
                return;
              }
            }
            toolWindow.remove();
          }
        }
      }
    }, project);

    FacetDependentToolWindow.EXTENSION_POINT_NAME.addExtensionPointListener(new ExtensionPointListener<FacetDependentToolWindow>() {
      @Override
      public void extensionAdded(@NotNull FacetDependentToolWindow extension, @NotNull PluginDescriptor pluginDescriptor) {
        initToolWindowIfNeeded(extension, project);
      }

      @Override
      public void extensionRemoved(@NotNull FacetDependentToolWindow extension, @NotNull PluginDescriptor pluginDescriptor) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(extension.id);
        if (window != null) {
          window.remove();
        }
      }
    }, project);
  }

  private static void initToolWindowIfNeeded(@NotNull FacetDependentToolWindow extension, @NotNull Project project) {
    ProjectFacetManager projectFacetManager = ProjectFacetManager.getInstance(project);
    for (FacetType<?, ?> type : extension.getFacetTypes()) {
      if (projectFacetManager.hasFacets(type.getId())) {
        ensureToolWindowExists(extension, project);
        return;
      }
    }
  }

  private static void ensureToolWindowExists(@NotNull FacetDependentToolWindow extension, @NotNull Project project) {
    ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(extension.id);
    if (toolWindow == null) {
      toolWindowManager.initToolWindow(extension);
    }
  }

  private static @NotNull List<FacetDependentToolWindow> getDependentExtensions(@NotNull Facet<?> facet) {
    return ContainerUtil.filter(FacetDependentToolWindow.EXTENSION_POINT_NAME.getExtensionList(), toolWindowEP -> {
      for (String id : toolWindowEP.getFacetIds()) {
        if (facet.getType().getStringId().equals(id)) {
          return true;
        }
      }
      return false;
    });
  }
}
