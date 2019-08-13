/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.artifact.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.JpsArtifactReference;
import org.jetbrains.jps.model.impl.JpsNamedElementReferenceImpl;

/**
 * @author nik
 */
public class JpsArtifactReferenceImpl extends JpsNamedElementReferenceImpl<JpsArtifact,JpsArtifactReferenceImpl> implements JpsArtifactReference {
  public JpsArtifactReferenceImpl(@NotNull String artifactName) {
    super(JpsArtifactRole.ARTIFACT_COLLECTION_ROLE, artifactName, JpsElementFactory.getInstance().createProjectReference());
  }

  private JpsArtifactReferenceImpl(JpsArtifactReferenceImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsArtifactReferenceImpl createCopy() {
    return new JpsArtifactReferenceImpl(this);
  }

  @NotNull
  @Override
  public String getArtifactName() {
    return myElementName;
  }

  @Override
  public JpsArtifactReferenceImpl asExternal(@NotNull JpsModel model) {
    model.registerExternalReference(this);
    return this;
  }
}
