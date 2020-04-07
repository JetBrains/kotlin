// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl.ui;

import com.intellij.facet.*;
import com.intellij.facet.ui.FacetDependentToolWindow;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class FacetDependentToolWindowManager implements ProjectComponent {
  private final Project myProject;

  private FacetDependentToolWindowManager(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    ProjectWideFacetListenersRegistry.getInstance(myProject).registerListener(new ProjectWideFacetAdapter<Facet>() {
      @Override
      public void facetAdded(@NotNull Facet facet) {
        for (FacetDependentToolWindow extension : getDependentExtensions(facet)) {
          ensureToolWindowExists(extension);
        }
      }

      @Override
      public void facetRemoved(@NotNull Facet facet) {
        ProjectFacetManager facetManager = ProjectFacetManager.getInstance(myProject);
        if (facetManager.hasFacets(facet.getTypeId())) {
          return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
        for (FacetDependentToolWindow extension : getDependentExtensions(facet)) {
          ToolWindow toolWindow = toolWindowManager.getToolWindow(extension.id);
          if (toolWindow != null) {
            // check for other facets
            List<FacetType> facetTypes = extension.getFacetTypes();
            for (FacetType facetType : facetTypes) {
              if (facetManager.hasFacets(facetType.getId())) {
                return;
              }
            }
            toolWindow.remove();
          }
        }
      }
    }, myProject);

    for (FacetDependentToolWindow extension : FacetDependentToolWindow.EXTENSION_POINT_NAME.getExtensionList()) {
      initToolWindowIfNeeded(extension);
    }

    FacetDependentToolWindow.EXTENSION_POINT_NAME.addExtensionPointListener(new ExtensionPointListener<FacetDependentToolWindow>() {
      @Override
      public void extensionAdded(@NotNull FacetDependentToolWindow extension, @NotNull PluginDescriptor pluginDescriptor) {
        initToolWindowIfNeeded(extension);
      }

      @Override
      public void extensionRemoved(@NotNull FacetDependentToolWindow extension, @NotNull PluginDescriptor pluginDescriptor) {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(extension.id);
        if (window != null) {
          window.remove();
        }
      }
    }, myProject);
  }

  private void initToolWindowIfNeeded(FacetDependentToolWindow extension) {
    for (FacetType<?, ?> type : extension.getFacetTypes()) {
      if (ProjectFacetManager.getInstance(myProject).hasFacets(type.getId())) {
        ensureToolWindowExists(extension);
        return;
      }
    }
  }

  private void ensureToolWindowExists(FacetDependentToolWindow extension) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(extension.id);
    if (toolWindow == null) {
      ToolWindowManagerEx.getInstanceEx(myProject).initToolWindow(extension);
    }
  }

  private static List<FacetDependentToolWindow> getDependentExtensions(final Facet facet) {
    return ContainerUtil.filter(FacetDependentToolWindow.EXTENSION_POINT_NAME.getExtensionList(), toolWindowEP -> {
      for (String id : toolWindowEP.getFacetIds()) {
        if (facet.getType().getStringId().equals(id)) return true;
      }
      return false;
    });
  }
}
