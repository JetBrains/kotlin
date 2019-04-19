/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.openapi.roots.ProjectModelExternalSource;
import com.intellij.packaging.elements.PackagingElementFactory;
import org.jetbrains.jps.model.serialization.artifact.ArtifactState;

/**
 * @author nik
 */
public class InvalidArtifact extends ArtifactImpl {
  private final ArtifactState myState;
  private final String myErrorMessage;

  public InvalidArtifact(ArtifactState state, String errorMessage, ProjectModelExternalSource externalSource) {
    super(state.getName(), InvalidArtifactType.getInstance(), false, PackagingElementFactory.getInstance().createArtifactRootElement(), "",
          externalSource);
    myState = state;
    myErrorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return myErrorMessage;
  }

  public ArtifactState getState() {
    return myState;
  }
}
