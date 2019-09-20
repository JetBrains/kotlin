// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.compiler;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@State(name = "ArtifactsWorkspaceSettings",
  storages = {
    @Storage(StoragePathMacros.WORKSPACE_FILE)
  })
public class ArtifactsWorkspaceSettings implements PersistentStateComponent<ArtifactsWorkspaceSettings.ArtifactsWorkspaceSettingsState> {
  private ArtifactsWorkspaceSettingsState myState = new ArtifactsWorkspaceSettingsState();
  private final Project myProject;

  public ArtifactsWorkspaceSettings(Project project) {
    myProject = project;
  }

  public static ArtifactsWorkspaceSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ArtifactsWorkspaceSettings.class);
  }

  public List<Artifact> getArtifactsToBuild() {
    final List<Artifact> result = new ArrayList<>();
    final ArtifactManager artifactManager = ArtifactManager.getInstance(myProject);
    for (String name : myState.myArtifactsToBuild) {
      ContainerUtil.addIfNotNull(result, artifactManager.findArtifact(name));
    }
    return result;
  }

  public void setArtifactsToBuild(@NotNull Collection<? extends Artifact> artifacts) {
    myState.myArtifactsToBuild.clear();
    for (Artifact artifact : artifacts) {
      myState.myArtifactsToBuild.add(artifact.getName());
    }
    Collections.sort(myState.myArtifactsToBuild);
  }

  @Override
  public ArtifactsWorkspaceSettingsState getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull ArtifactsWorkspaceSettingsState state) {
    myState = state;
  }

  public static class ArtifactsWorkspaceSettingsState {
    @XCollection(propertyElementName = "artifacts-to-build", elementName = "artifact", valueAttributeName = "name")
    public List<String> myArtifactsToBuild = new ArrayList<>();

  }
}
