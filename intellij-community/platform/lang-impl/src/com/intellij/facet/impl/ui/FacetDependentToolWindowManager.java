// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl.ui;

import com.intellij.facet.*;
import com.intellij.facet.ui.FacetDependentToolWindow;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
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

        ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(myProject);
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
            toolWindowManager.unregisterToolWindow(extension.id);
          }
        }
      }
    }, myProject);

    ProjectFacetManager facetManager = ProjectFacetManager.getInstance(myProject);
    loop: for (FacetDependentToolWindow extension : FacetDependentToolWindow.EXTENSION_POINT_NAME.getExtensionList()) {
      for (FacetType type : extension.getFacetTypes()) {
        if (facetManager.hasFacets(type.getId())) {
          ensureToolWindowExists(extension);
          continue loop;
        }
      }
    }
  }

  private void ensureToolWindowExists(FacetDependentToolWindow extension) {
    ToolWindow toolWindow = ToolWindowManagerEx.getInstanceEx(myProject).getToolWindow(extension.id);
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
