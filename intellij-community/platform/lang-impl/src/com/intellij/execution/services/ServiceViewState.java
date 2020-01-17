// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.util.SmartList;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;

import javax.swing.tree.TreePath;
import java.util.List;

@Tag("serviceView")
final class ServiceViewState {
  private static final float DEFAULT_CONTENT_PROPORTION = 0.3f;

  @Attribute("id")
  public String id = "";
  @Attribute("groupId")
  public String groupId = ToolWindowId.SERVICES;
  public float contentProportion = DEFAULT_CONTENT_PROPORTION;
  @Tag("treeState")
  public Element treeStateElement;

  public boolean groupByServiceGroups = true;
  public boolean groupByContributor;
  public boolean isSelected;

  public String viewType = "";
  public List<ServiceState> roots = new SmartList<>();
  public int parentView = -1;

  @Transient
  public TreeState treeState = TreeState.createFrom(null);
  @Transient
  public List<TreePath> expandedPaths = new SmartList<>();
  @Transient
  public boolean showServicesTree = true;

  public static final class ServiceState {
    public List<String> path;
    public String contributor;
  }
}
