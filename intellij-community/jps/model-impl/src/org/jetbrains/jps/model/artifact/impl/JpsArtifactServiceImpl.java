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
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementCollection;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.JpsArtifactReference;
import org.jetbrains.jps.model.artifact.JpsArtifactService;
import org.jetbrains.jps.model.artifact.JpsArtifactType;
import org.jetbrains.jps.model.artifact.elements.JpsCompositePackagingElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JpsArtifactServiceImpl extends JpsArtifactService {

  @Override
  public List<JpsArtifact> getArtifacts(@NotNull JpsProject project) {
    JpsElementCollection<JpsArtifact> collection = project.getContainer().getChild(JpsArtifactRole.ARTIFACT_COLLECTION_ROLE);
    return collection != null ? collection.getElements() : Collections.emptyList();
  }

  @Override
  public List<JpsArtifact> getSortedArtifacts(@NotNull JpsProject project) {
    List<JpsArtifact> artifacts = new ArrayList<>(getArtifacts(project));
    artifacts.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    return artifacts;
  }

  @Override
  public <P extends JpsElement> JpsArtifact addArtifact(@NotNull JpsProject project,
                                                        @NotNull String name,
                                                        @NotNull JpsCompositePackagingElement rootElement,
                                                        @NotNull JpsArtifactType<P> type,
                                                        @NotNull P properties) {
    JpsArtifact artifact = createArtifact(name, rootElement, type, properties);
    return project.getContainer().getOrSetChild(JpsArtifactRole.ARTIFACT_COLLECTION_ROLE).addChild(artifact);
  }


  @Override
  public <P extends JpsElement> JpsArtifact createArtifact(@NotNull String name, @NotNull JpsCompositePackagingElement rootElement,
                                                           @NotNull JpsArtifactType<P> type, @NotNull P properties) {
    return new JpsArtifactImpl<>(name, rootElement, type, properties);
  }

  @Override
  public JpsArtifactReference createReference(@NotNull String artifactName) {
    return new JpsArtifactReferenceImpl(artifactName);
  }
}
