// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

public class FacetDependentToolWindowManager implements ProjectComponent {
  private final Project myProject;
  private final ProjectWideFacetListenersRegistry myFacetListenersRegistry;
  private final ProjectFacetManager myFacetManager;
  private final ToolWindowManagerEx myToolWindowManager;

  protected FacetDependentToolWindowManager(Project project,
                                            ProjectWideFacetListenersRegistry facetListenersRegistry,
                                            ProjectFacetManager facetManager,
                                            ToolWindowManagerEx toolWindowManager) {
    myProject = project;
    myFacetListenersRegistry = facetListenersRegistry;
    myFacetManager = facetManager;
    myToolWindowManager = toolWindowManager;
  }

  @Override
  public void projectOpened() {
    myFacetListenersRegistry.registerListener(new ProjectWideFacetAdapter<Facet>() {
      @Override
      public void facetAdded(@NotNull Facet facet) {
        for (FacetDependentToolWindow extension : getDependentExtensions(facet)) {
          ensureToolWindowExists(extension);
        }
      }

      @Override
      public void facetRemoved(@NotNull Facet facet) {
        if (!myFacetManager.hasFacets(facet.getTypeId())) {
          for (FacetDependentToolWindow extension : getDependentExtensions(facet)) {
            ToolWindow toolWindow = myToolWindowManager.getToolWindow(extension.id);
            if (toolWindow != null) {
              // check for other facets
              List<FacetType> facetTypes = extension.getFacetTypes();
              for (FacetType facetType : facetTypes) {
                if (myFacetManager.hasFacets(facetType.getId())) return;
              }
              myToolWindowManager.unregisterToolWindow(extension.id);
            }
          }
        }
      }
    }, myProject);

    loop: for (FacetDependentToolWindow extension : FacetDependentToolWindow.EXTENSION_POINT_NAME.getExtensionList()) {
      for (FacetType type : extension.getFacetTypes()) {
        if (myFacetManager.hasFacets(type.getId())) {
          ensureToolWindowExists(extension);
          continue loop;
        }
      }
    }
  }

  private void ensureToolWindowExists(FacetDependentToolWindow extension) {
    ToolWindow toolWindow = myToolWindowManager.getToolWindow(extension.id);
    if (toolWindow == null) {
      myToolWindowManager.initToolWindow(extension);
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
