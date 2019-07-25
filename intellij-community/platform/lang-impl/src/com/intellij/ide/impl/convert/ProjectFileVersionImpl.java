// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.impl.convert;

import com.intellij.conversion.ConversionService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.project.ProjectKt;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
@State(name = ProjectFileVersionImpl.COMPONENT_NAME)
public class ProjectFileVersionImpl extends ProjectFileVersion implements Disposable, PersistentStateComponent<ProjectFileVersionState> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.impl.convert.ProjectFileVersionImpl");
  @NonNls public static final String COMPONENT_NAME = "ProjectFileVersion";
  private final Project myProject;
  private final ProjectFileVersionState myState = new ProjectFileVersionState();

  public ProjectFileVersionImpl(Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
    if (myProject.isDefault() || ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    String path = ProjectKt.isDirectoryBased(myProject) ? myProject.getBasePath() : myProject.getProjectFilePath();
    if (path == null) {
      LOG.info("Cannot save conversion result: filePath == null");
    }
    else {
      ConversionService.getInstance().saveConversionResult(FileUtil.toSystemDependentName(path));
    }
  }

  @Override
  public ProjectFileVersionState getState() {
    if (myState != null && !myState.getPerformedConversionIds().isEmpty()) {
      return myState;
    }
    return null;
  }

  @Override
  public void loadState(@NotNull final ProjectFileVersionState state) {
    XmlSerializerUtil.copyBean(state, myState);
  }
}
