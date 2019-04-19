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

import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

/**
* @author nik
*/
public class JpsArtifactRole extends JpsElementChildRoleBase<JpsArtifact> {
  private static final JpsArtifactRole INSTANCE = new JpsArtifactRole();
  public static final JpsElementCollectionRole<JpsArtifact> ARTIFACT_COLLECTION_ROLE = JpsElementCollectionRole.create(INSTANCE);

  private JpsArtifactRole() {
    super("artifact");
  }
}
