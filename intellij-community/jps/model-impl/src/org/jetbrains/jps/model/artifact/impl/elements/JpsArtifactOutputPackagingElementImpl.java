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
package org.jetbrains.jps.model.artifact.impl.elements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.JpsArtifactReference;
import org.jetbrains.jps.model.artifact.elements.JpsArtifactOutputPackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsArtifactRootElement;
import org.jetbrains.jps.model.artifact.elements.JpsCompositePackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;
import org.jetbrains.jps.model.artifact.elements.ex.JpsComplexPackagingElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JpsArtifactOutputPackagingElementImpl extends JpsComplexPackagingElementBase<JpsArtifactOutputPackagingElementImpl>
  implements JpsArtifactOutputPackagingElement {
  private static final JpsElementChildRole<JpsArtifactReference>
    ARTIFACT_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("artifact reference");

  public JpsArtifactOutputPackagingElementImpl(@NotNull JpsArtifactReference reference) {
    myContainer.setChild(ARTIFACT_REFERENCE_CHILD_ROLE, reference);
  }

  private JpsArtifactOutputPackagingElementImpl(JpsArtifactOutputPackagingElementImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsArtifactOutputPackagingElementImpl createCopy() {
    return new JpsArtifactOutputPackagingElementImpl(this);
  }

  @Override
  @NotNull
  public JpsArtifactReference getArtifactReference() {
    return myContainer.getChild(ARTIFACT_REFERENCE_CHILD_ROLE);
  }

  @Override
  public List<JpsPackagingElement> getSubstitution() {
    JpsArtifact artifact = getArtifactReference().resolve();
    if (artifact == null) return Collections.emptyList();
    JpsCompositePackagingElement rootElement = artifact.getRootElement();
    if (rootElement instanceof JpsArtifactRootElement) {
      return new ArrayList<>(rootElement.getChildren());
    }
    else {
      return Collections.singletonList(rootElement);
    }
  }
}
