/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  private boolean singleModuleProject = false;

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
  protected List<? extends ExternalSystemNode> doBuildChildren() {
    setIdeGrouping(null);
    final List<? extends ExternalSystemNode> children = super.doBuildChildren();
    final List<ExternalSystemNode> visibleChildren = ContainerUtil.filter(children, node -> node.isVisible());
    if (visibleChildren.size() == 1 && visibleChildren.get(0).getName().equals(getName())) {
      singleModuleProject = true;
      final ExternalSystemNode node = visibleChildren.get(0);
      if (node instanceof ModuleNode) {
        setIdeGrouping(((ModuleNode)node).getIdeGrouping());
      }
      //noinspection unchecked
      return node.doBuildChildren();
    }
    else {
      singleModuleProject = false;
      return visibleChildren;
    }
  }

  public boolean isSingleModuleProject() {
    return singleModuleProject;
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
}
