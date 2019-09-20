// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ProjectNode extends ExternalSystemNode<ProjectData> {
  private String myTooltipCache;
  private ModuleNode effectiveRoot = null;

  public ProjectNode(ExternalProjectsView externalProjectsView, DataNode<ProjectData> projectDataNode) {
    super(externalProjectsView, null, projectDataNode);
    updateProject();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(getUiAware().getProjectIcon());
  }

  public ExternalSystemNode getGroup() {
    return (ExternalSystemNode)getParent();
  }

  @NotNull
  @Override
  protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
    setIdeGrouping(null);
    final List<? extends ExternalSystemNode<?>> children = super.doBuildChildren();
    final List<ExternalSystemNode<?>> visibleChildren = ContainerUtil.filter(children, node -> node.isVisible());
    if (getExternalProjectsView().getGroupModules()) {
      final List<ExternalSystemNode<?>> topLevelChildren =
        ContainerUtil.filter(visibleChildren, node -> !(node instanceof ModuleNode) || ((ModuleNode)node).getIdeParentGrouping() == null);
      if (topLevelChildren.size() == 1) {
        ExternalSystemNode<?> child = topLevelChildren.get(0);
        if (child instanceof ModuleNode) {
          effectiveRoot = (ModuleNode)child;
          return effectiveRoot.doBuildChildren();
        }
      }
      return topLevelChildren;
    }

    effectiveRoot = null;
    return visibleChildren;
  }

  void updateProject() {
    myTooltipCache = makeDescription();
    getStructure().updateFrom(getParent());
  }

  @Override
  protected void doUpdate() {
    String autoImportHint = null;
    final ProjectData projectData = getData();
    if (projectData != null) {
      final AbstractExternalSystemSettings externalSystemSettings =
        ExternalSystemApiUtil.getSettings(getExternalProjectsView().getProject(), getData().getOwner());
      final ExternalProjectSettings projectSettings =
        externalSystemSettings.getLinkedProjectSettings(projectData.getLinkedExternalProjectPath());
      if (projectSettings != null && projectSettings.isUseAutoImport()) autoImportHint = "auto-import enabled";
    }

    setNameAndTooltip(getName(), myTooltipCache, autoImportHint);
  }

  private String makeDescription() {
    StringBuilder desc = new StringBuilder();
    final ProjectData projectData = getData();
    desc
      .append("Project: ").append(getName())
      .append(projectData != null ?
              "\n\rLocation: " + projectData.getLinkedExternalProjectPath() : "")
      .append(projectData != null && !StringUtil.isEmptyOrSpaces(projectData.getDescription()) ?
              "\n\r" + projectData.getDescription() : "");
    return desc.toString();
  }

  @Nullable
  public String getIdeGrouping() {
    ProjectData data = getData();
    if (data == null) return null;
    return data.getIdeGrouping();
  }

  private void setIdeGrouping(@Nullable String ideGrouping) {
    ProjectData data = getData();
    if (data != null) {
      data.setIdeGrouping(ideGrouping);
    }
  }

  @Override
  @Nullable
  @NonNls
  protected String getMenuId() {
    return "ExternalSystemView.ProjectMenu";
  }

  public ModuleNode getEffectiveRoot() {
    return effectiveRoot;
  }
}
