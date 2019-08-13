/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ConfigurationErrorDescription;
import com.intellij.openapi.module.ConfigurationErrorType;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;

/**
 * @author nik
 */
public class ArtifactLoadingErrorDescription extends ConfigurationErrorDescription {
  private static final ConfigurationErrorType INVALID_ARTIFACT = new ConfigurationErrorType("artifact", false);
  private final Project myProject;
  private final InvalidArtifact myArtifact;

  public ArtifactLoadingErrorDescription(Project project, InvalidArtifact artifact) {
    super(artifact.getName(), artifact.getErrorMessage(), INVALID_ARTIFACT);
    myProject = project;
    myArtifact = artifact;
  }

  @Override
  public void ignoreInvalidElement() {
    final ModifiableArtifactModel model = ArtifactManager.getInstance(myProject).createModifiableModel();
    model.removeArtifact(myArtifact);
    WriteAction.run(() -> model.commit());
  }

  @Override
  public String getIgnoreConfirmationMessage() {
    return "Would you like to remove artifact '" + myArtifact.getName() + "?";
  }

  @Override
  public boolean isValid() {
    return ArtifactManager.getInstance(myProject).getAllArtifactsIncludingInvalid().contains(myArtifact);
  }
}
