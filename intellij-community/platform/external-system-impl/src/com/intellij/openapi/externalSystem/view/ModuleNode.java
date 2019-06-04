// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_MODULE_DATA_NODE_ORDER)
public class ModuleNode extends ExternalSystemNode<ModuleData> {
  private final boolean myIsRoot;
  private final ModuleData myData;
  // registry of all modules since we can't use getExternalProjectsView for getting modules withing a module
  private Collection<ModuleNode> myAllModules = Collections.emptyList();
  private final RunConfigurationsNode myRunConfigurationsNode;

  public ModuleNode(ExternalProjectsView externalProjectsView,
                    DataNode<ModuleData> dataNode,
                    @Nullable ExternalSystemNode parent,
                    boolean isRoot) {
    super(externalProjectsView, parent, dataNode);
    myIsRoot = isRoot;
    myData = dataNode.getData();
    myRunConfigurationsNode = new RunConfigurationsNode(externalProjectsView, this);
  }

  public void setAllModules(Collection<ModuleNode> allModules) {
    myAllModules = allModules;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(getUiAware().getProjectIcon());

    String hint = null;
    if (myIsRoot) {
      hint = "root";
    }

    final String tooltip = myData.toString() + (myData.getDescription() != null ? "<br>" + myData.getDescription() : "");
    setNameAndTooltip(getName(), tooltip, hint);
  }

  @NotNull
  @Override
  protected List<? extends ExternalSystemNode> doBuildChildren() {
    List<ExternalSystemNode<?>> myChildNodes = new ArrayList<>();
    if (getExternalProjectsView().getGroupModules()) {
      List<ModuleNode> childModules = ContainerUtil.findAll(
        myAllModules,
        module -> module != this && StringUtil.equals(module.getIdeParentGrouping(), getIdeGrouping())
      );
      myChildNodes.addAll(childModules);
    }
    //noinspection unchecked
    myChildNodes.addAll((Collection<? extends ExternalSystemNode<?>>)super.doBuildChildren());
    myChildNodes.add(myRunConfigurationsNode);
    return myChildNodes;
  }

  @Nullable
  @Override
  protected String getMenuId() {
    return "ExternalSystemView.ModuleMenu";
  }

  @Override
  public int compareTo(@NotNull ExternalSystemNode node) {
    return myIsRoot ? -1 : (node instanceof ModuleNode && ((ModuleNode)node).myIsRoot) ? 1 : super.compareTo(node);
  }

  public void updateRunConfigurations() {
    myRunConfigurationsNode.updateRunConfigurations();
    childrenChanged();
    getExternalProjectsView().updateUpTo(this);
    getExternalProjectsView().updateUpTo(myRunConfigurationsNode);
  }

  @Override
  public String getName() {
    if (getExternalProjectsView().getGroupModules()) {
      return myData.getExternalName();
    }
    return super.getName();
  }

  @Override
  public boolean isVisible() {
    if (!myIsRoot && getExternalProjectsView().getGroupModules()) {
      ModuleNode parentModule = findParent(ModuleNode.class);
      if (parentModule != null) {
        return StringUtil.equals(parentModule.getIdeGrouping(), getIdeParentGrouping());
      }
      ProjectNode parentProject = findParent(ProjectNode.class);
      if (parentProject != null) {
        return StringUtil.equals(parentProject.getIdeGrouping(), getIdeParentGrouping());
      }
    }
    return super.isVisible();
  }

  @Nullable
  public String getIdeGrouping() {
    ModuleData data = getData();
    if (data == null) return null;
    return data.getIdeGrouping();
  }

  @Nullable
  public String getIdeParentGrouping() {
    ModuleData data = getData();
    if (data == null) return null;
    return data.getIdeParentGrouping();
  }

  @Override
  public void mergeWith(ExternalSystemNode<ModuleData> node) {
    super.mergeWith(node);
    ModuleNode moduleNode = node instanceof ModuleNode ? ((ModuleNode)node) : null;
    if (moduleNode != null) {
      myAllModules  = moduleNode.myAllModules;
    }
  }
}
