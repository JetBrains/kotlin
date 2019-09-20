// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;

import javax.swing.*;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public abstract class TasksViewManager extends AbstractViewManager {
  public TasksViewManager(Project project) {
    super(project);
  }

  @Override
  protected void onBuildStart(BuildDescriptor buildDescriptor) {
    BuildInfo buildInfo = (BuildInfo)buildDescriptor;
    Content content = buildInfo.content;
    Map<BuildDescriptor, BuildView> buildsMap = getBuildsMap();
    String tabName = buildsMap.size() > 1 ? getViewName() : getViewName() + ": " + buildInfo.getTitle();
    ((BuildContentManagerImpl)myBuildContentManager).updateTabDisplayName(content, tabName);
  }

  @Override
  protected Icon getContentIcon() {
    return AllIcons.General.GearPlain;
  }

  @Override
  public boolean isConsoleEnabledByDefault() {
    return true;
  }
}
