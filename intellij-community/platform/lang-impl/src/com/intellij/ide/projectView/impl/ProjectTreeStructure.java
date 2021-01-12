// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 * */

public abstract class ProjectTreeStructure extends AbstractProjectTreeStructure {
  private final String myId;

  public ProjectTreeStructure(@NotNull Project project, final String ID) {
    super(project);
    myId = ID;
  }

  @Override
  public boolean isFlattenPackages() {
    return ProjectView.getInstance(myProject).isFlattenPackages(myId);
  }

  @Override
  public boolean isShowMembers() {
    return ProjectView.getInstance(myProject).isShowMembers(myId);
  }

  @Override
  public boolean isHideEmptyMiddlePackages() {
    return ProjectView.getInstance(myProject).isHideEmptyMiddlePackages(myId);
  }

  @Override
  public boolean isCompactDirectories() {
    return ProjectView.getInstance(myProject).isCompactDirectories(myId);
  }

  @Override
  public boolean isAbbreviatePackageNames() {
    return ProjectView.getInstance(myProject).isAbbreviatePackageNames(myId);
  }

  @Override
  public boolean isShowLibraryContents() {
    return ProjectView.getInstance(myProject).isShowLibraryContents(myId);
  }

  @Override
  public boolean isShowModules() {
    return ProjectView.getInstance(myProject).isShowModules(myId);
  }

  @Override
  public boolean isFlattenModules() {
    return ProjectView.getInstance(myProject).isFlattenModules(myId);
  }

  @Override
  public boolean isShowURL() {
    return ProjectView.getInstance(myProject).isShowURL(myId);
  }
}